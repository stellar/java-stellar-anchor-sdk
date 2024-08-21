package org.stellar.anchor.platform.callback;

import static java.lang.Math.abs;
import static okhttp3.HttpUrl.get;
import static org.stellar.anchor.util.ErrorHelper.logErrorAndThrow;
import static org.stellar.anchor.util.Log.*;
import static org.stellar.anchor.util.NumberHelper.isPositiveNumber;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Objects;
import okhttp3.HttpUrl;
import okhttp3.HttpUrl.Builder;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.http.HttpStatus;
import org.stellar.anchor.api.callback.GetRateRequest;
import org.stellar.anchor.api.callback.GetRateResponse;
import org.stellar.anchor.api.callback.RateIntegration;
import org.stellar.anchor.api.exception.AnchorException;
import org.stellar.anchor.api.exception.ServerErrorException;
import org.stellar.anchor.api.sep.AssetInfo;
import org.stellar.anchor.api.shared.FeeDescription;
import org.stellar.anchor.api.shared.FeeDetails;
import org.stellar.anchor.asset.AssetService;
import org.stellar.anchor.auth.AuthHelper;
import org.stellar.anchor.util.NumberHelper;

public class RestRateIntegration implements RateIntegration {
  private static final RoundingMode[] ALLOWED_ROUNDING_MODES_FOR_QUOTE_VALIDATION =
      new RoundingMode[] {
        RoundingMode.FLOOR, RoundingMode.CEILING, RoundingMode.HALF_UP, RoundingMode.HALF_DOWN
      };
  private final String anchorEndpoint;
  private final OkHttpClient httpClient;
  private final Gson gson;
  private final AuthHelper authHelper;
  private final AssetService assetService;

  public RestRateIntegration(
      String anchorEndpoint,
      OkHttpClient httpClient,
      AuthHelper authHelper,
      Gson gson,
      AssetService assetService) {
    try {
      new URI(anchorEndpoint);
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException("invalid 'baseUri'");
    }

    this.anchorEndpoint = anchorEndpoint;
    this.httpClient = httpClient;
    this.authHelper = authHelper;
    this.gson = gson;
    this.assetService = assetService;
  }

  @Override
  public GetRateResponse getRate(GetRateRequest request) throws AnchorException {
    Builder urlBuilder = get(anchorEndpoint).newBuilder().addPathSegment("rate");
    Type type = new TypeToken<Map<String, ?>>() {}.getType();
    Map<String, String> paramsMap = gson.fromJson(gson.toJson(request), type);
    paramsMap.forEach(
        (key, value) -> {
          if (value != null) {
            urlBuilder.addQueryParameter(key, value);
          }
        });
    HttpUrl url = urlBuilder.build();

    Request httpRequest =
        PlatformIntegrationHelper.getRequestBuilder(authHelper).url(url).get().build();
    Response response = PlatformIntegrationHelper.call(httpClient, httpRequest);
    String responseContent = PlatformIntegrationHelper.getContent(response);

    if (response.code() != HttpStatus.OK.value()) {
      throw PlatformIntegrationHelper.httpError(responseContent, response.code(), gson);
    }

    GetRateResponse getRateResponse;
    try {
      getRateResponse = gson.fromJson(responseContent, GetRateResponse.class);
    } catch (Exception e) { // cannot read body from response
      errorEx("Error parsing body response to GetRateResponse", e);
      throw new ServerErrorException("internal server error", e);
    }

    validateRateResponse(request, getRateResponse);
    return getRateResponse;
  }

  void validateRateResponse(GetRateRequest request, GetRateResponse getRateResponse)
      throws ServerErrorException {
    AssetInfo sellAsset = assetService.getAssetByName(request.getSellAsset());
    AssetInfo buyAsset = assetService.getAssetByName(request.getBuyAsset());

    GetRateResponse.Rate rate = getRateResponse.getRate();
    if (rate == null || rate.getPrice() == null) {
      logErrorAndThrow("missing 'price' in the GET /rate response", ServerErrorException.class);
    }

    if (request.getType() == GetRateRequest.Type.FIRM) {
      if (Objects.requireNonNull(rate).getFee() == null) {
        logErrorAndThrow("'fee' is missing in the GET /rate response", ServerErrorException.class);
      }
      if (rate.getId() == null || rate.getExpiresAt() == null) {
        logErrorAndThrow(
            "'id' and/or 'expires_at' are missing in the GET /rate response",
            ServerErrorException.class);
      }
    }

    // sell_amount is present and positive number
    if (!isPositiveNumber(Objects.requireNonNull(rate).getSellAmount())) {
      logErrorAndThrow(
          "'sell_amount' is missing or not a positive number in the GET /rate response",
          ServerErrorException.class);
    }

    // buy_amount is present and positive number
    if (!isPositiveNumber(rate.getBuyAmount())) {
      logErrorAndThrow(
          "'buy_amount' is missing or not a positive number in the GET /rate response",
          ServerErrorException.class);
    }

    // sell_amount has a proper number of significant decimals
    if (!NumberHelper.hasProperSignificantDecimals(
        Objects.requireNonNull(rate).getSellAmount(), sellAsset.getSignificantDecimals())) {
      logErrorAndThrow(
          "'sell_amount' has incorrect number of significant decimals in the GET /rate response",
          ServerErrorException.class);
    }

    // buy_amount has a proper number of significant decimals
    if (!NumberHelper.hasProperSignificantDecimals(
        Objects.requireNonNull(rate).getBuyAmount(), buyAsset.getSignificantDecimals())) {
      logErrorAndThrow(
          "'buy_amount' has incorrect number of significant decimals in the GET /rate response",
          ServerErrorException.class);
    }

    FeeDetails fee = rate.getFee();
    // if fee is set, check the following
    if (fee != null) {
      // fee.total is present and is a positive number
      if (!isPositiveNumber(fee.getTotal())) {
        logErrorAndThrow(
            "'fee.total' is missing or not a positive number in the GET /rate response",
            ServerErrorException.class);
      }
      // fee.asset is a valid asset
      AssetInfo feeAsset = assetService.getAssetByName(fee.getAsset());
      if (fee.getAsset() == null || feeAsset == null) {
        logErrorAndThrow(
            "'fee.asset' is missing or not a valid asset in the GET /rate response",
            ServerErrorException.class);
      }

      if (fee.getAsset().equals(request.getSellAsset())) {
        // when fee is in sell_asset,
        // check that sell_amount ~= price * buy_amount + (fee ?: 0)
        BigDecimal expected =
            new BigDecimal(rate.getPrice())
                .multiply(new BigDecimal(rate.getBuyAmount()))
                .add(new BigDecimal(fee.getTotal()));

        // Since we don't know how the anchor rounds the amounts, we need to check the equality in
        // all allowed rounding modes
        if (!withinRoundingError(
            new BigDecimal(rate.getSellAmount()), expected, sellAsset.getSignificantDecimals())) {
          logErrorAndThrow(
              "'sell_amount' is not within rounding error of price * buy_amount + (fee?:0) in the GET /rate response",
              ServerErrorException.class);
        }
      } else {
        // when fee is in buy_asset,
        // check that sell_amount ~= price * (buy_amount + (fee ?: 0))
        BigDecimal expected =
            new BigDecimal(rate.getPrice())
                .multiply(new BigDecimal(rate.getBuyAmount()).add(new BigDecimal(fee.getTotal())));
        // Since we don't know how the anchor rounds the amounts, we need to check the equality in
        // all allowed rounding modes
        if (!withinRoundingError(
            new BigDecimal(rate.getSellAmount()), expected, sellAsset.getSignificantDecimals())) {
          logErrorAndThrow(
              "'sell_amount' is not within rounding error of price * (buy_amount + (fee ?: 0)) in the GET /rate response",
              ServerErrorException.class);
        }
      }

      if (fee.getDetails() != null) {
        BigDecimal totalFee = new BigDecimal(0);
        for (FeeDescription feeDescription : fee.getDetails()) {
          if (!isPositiveNumber(feeDescription.getAmount())) {
            logErrorAndThrow(
                "'fee.details[?].description.amount' is missing or not a positive number in the GET /rate response",
                ServerErrorException.class);
          }

          if (!NumberHelper.hasProperSignificantDecimals(
              feeDescription.getAmount(), feeAsset.getSignificantDecimals())) {
            logErrorAndThrow(
                "'fee.details[?].description.amount' has incorrect number of significant decimals in the GET /rate response",
                ServerErrorException.class);
          }
          if (feeDescription.getName() == null) {
            logErrorAndThrow(
                "'fee.details.description[?].name' is missing in the GET /rate response",
                ServerErrorException.class);
          }
          totalFee = totalFee.add(new BigDecimal(feeDescription.getAmount()));
        }

        // check that sell_amount is equal to price * buy_amount + (fee ?: 0)
        if (totalFee.compareTo(new BigDecimal(fee.getTotal())) != 0) {
          logErrorAndThrow(
              "'fee.total' is not equal to the sum of fees in the GET /rate response",
              ServerErrorException.class);
        }
      }
    } else {
      // when fee is not present, check that sell_amount is equal to price * buy_amount
      BigDecimal expected =
          new BigDecimal(rate.getPrice()).multiply(new BigDecimal(rate.getBuyAmount()));
      // Since we don't know how the anchor rounds the amounts, we need to check the equality in
      // all allowed rounding modes
      if (withinRoundingError(
          new BigDecimal(rate.getSellAmount()), expected, sellAsset.getSignificantDecimals())) {
        logErrorAndThrow(
            "'sell_amount' is not equal to price * buy_amount in the GET /rate response",
            ServerErrorException.class);
      }
    }
  }

  /**
   * Check the amount is within rounding error of the expected amount.
   *
   * @param amount The amount to be compared
   * @param expected The expected amount
   * @param scale The scale to be used for comparison
   * @return true if the amount is within rounding error of the expected amount
   */
  static boolean withinRoundingError(BigDecimal amount, BigDecimal expected, int scale) {
    return abs(amount.subtract(expected).doubleValue()) < Math.pow(10, -scale);
  }
}

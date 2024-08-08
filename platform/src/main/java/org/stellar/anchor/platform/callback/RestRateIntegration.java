package org.stellar.anchor.platform.callback;

import static okhttp3.HttpUrl.get;
import static org.stellar.anchor.util.ErrorHelper.logErrorAndThrow;
import static org.stellar.anchor.util.Log.*;
import static org.stellar.anchor.util.NumberHelper.isValidPositiveNumber;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
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
import org.stellar.anchor.api.shared.FeeDescription;
import org.stellar.anchor.api.shared.FeeDetails;
import org.stellar.anchor.asset.AssetService;
import org.stellar.anchor.auth.AuthHelper;

public class RestRateIntegration implements RateIntegration {
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

    validateResponse(request, getRateResponse);
    return getRateResponse;
  }

  private void validateResponse(GetRateRequest request, GetRateResponse getRateResponse)
      throws ServerErrorException {
    GetRateResponse.Rate rate = getRateResponse.getRate();
    if (rate == null || rate.getPrice() == null) {
      logErrorAndThrow("missing 'price' in the GET /rate response", ServerErrorException.class);
    }

    if (request.getType() != GetRateRequest.Type.INDICATIVE) {
      if (rate.getFee() == null) {
        logErrorAndThrow("'fee' is missing in the GET /rate response", ServerErrorException.class);
      }
    }

    if (request.getType() == GetRateRequest.Type.FIRM) {
      if (rate.getId() == null || rate.getExpiresAt() == null) {
        logErrorAndThrow(
            "'id' and/or 'expires_at' are missing in the GET /rate response",
            ServerErrorException.class);
      }
    }

    if (isValidPositiveNumber(rate.getSellAmount())) {
      logErrorAndThrow(
          "'sell_amount' is missing or not a positive number in the GET /rate response",
          ServerErrorException.class);
    }

    if (!isValidPositiveNumber(rate.getBuyAmount())) {
      logErrorAndThrow(
          "'buy_amount' is missing or not a positive number in the GET /rate response",
          ServerErrorException.class);
    }

    if (rate.getFee() != null) {
      FeeDetails fee = rate.getFee();
      // if fee is preset, fee.total is present and is a positive number
      if (!isValidPositiveNumber(fee.getTotal())) {
        logErrorAndThrow(
            "'fee.total' is missing or not a positive number in the GET /rate response",
            ServerErrorException.class);
      }

      if (fee.getAsset() == null || assetService.getAsset(fee.getAsset()) == null) {
        logErrorAndThrow(
            "'fee.asset' is missing or not a valid asset in the GET /rate response",
            ServerErrorException.class);
      }

      if (fee.getDetails() != null) {
        BigDecimal totalFee = new BigDecimal(0);
        for (FeeDescription feeDescription : fee.getDetails()) {
          if (feeDescription.getName() == null) {
            logErrorAndThrow(
                "'fee.details.description[?].name' is missing in the GET /rate response",
                ServerErrorException.class);
          }
          if (!isValidPositiveNumber(feeDescription.getAmount())) {
            logErrorAndThrow(
                "'fee.details[?].description.amount' is missing or not a positive number in the GET /rate response",
                ServerErrorException.class);
          }
          totalFee = totalFee.add(new BigDecimal(feeDescription.getAmount()));
        }
        if (!totalFee.equals(new BigDecimal(fee.getTotal()))) {
          logErrorAndThrow(
              "'fee.total' is not equal to the sum of 'fee.details[?].description.amount' in the GET /rate response",
              ServerErrorException.class);
        }
      }
    }

    // check that sell_amount is equal to price * buy_amount + (fee ?: 0)
    BigDecimal exptected =
        new BigDecimal(rate.getPrice()).multiply(new BigDecimal(rate.getBuyAmount()));
    if (rate.getFee() != null) {
      exptected = exptected.add(new BigDecimal(rate.getFee().getTotal()));
    }
    if (!rate.getSellAmount().equals(exptected.toString())) {
      logErrorAndThrow(
          "'sell_amount' is not equal to price * buy_amount + (fee?:0) in the GET /rate response",
          ServerErrorException.class);
    }
  }
}

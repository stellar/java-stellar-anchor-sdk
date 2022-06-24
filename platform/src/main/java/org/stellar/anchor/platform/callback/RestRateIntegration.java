package org.stellar.anchor.platform.callback;

import static okhttp3.HttpUrl.get;
import static org.stellar.anchor.platform.PlatformIntegrationHelper.*;

import com.google.gson.Gson;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Calendar;
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
import org.stellar.anchor.sep10.JwtService;
import org.stellar.anchor.sep10.JwtToken;
import org.stellar.anchor.util.Log;
import shadow.com.google.common.reflect.TypeToken;

public class RestRateIntegration implements RateIntegration {
  private final String anchorEndpoint;
  private final OkHttpClient httpClient;
  private final Gson gson;
  private final JwtService jwtService;
  private final long jwtExpirationMilliseconds;
  private final String issuerUrl;

  public RestRateIntegration(
      String anchorEndpoint,
      OkHttpClient httpClient,
      JwtService jwtService,
      String issuerUrl,
      long jwtExpirationMilliseconds,
      Gson gson) {
    try {
      new URI(anchorEndpoint);
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException("invalid 'baseUri'");
    }

    this.anchorEndpoint = anchorEndpoint;
    this.httpClient = httpClient;
    this.jwtService = jwtService;
    this.issuerUrl = issuerUrl;
    this.jwtExpirationMilliseconds = jwtExpirationMilliseconds;
    this.gson = gson;
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
        new Request.Builder()
            .url(url)
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + createJwtToken())
            .get()
            .build();
    Response response = call(httpClient, httpRequest);
    String responseContent = getContent(response);

    if (response.code() != HttpStatus.OK.value()) {
      throw httpError(responseContent, response.code(), gson);
    }

    GetRateResponse getRateResponse;
    try {
      getRateResponse = gson.fromJson(responseContent, GetRateResponse.class);
    } catch (Exception e) { // cannot read body from response
      Log.errorEx("Error parsing body response to GetRateResponse", e);
      throw new ServerErrorException("internal server error", e);
    }

    GetRateResponse.Rate rate = getRateResponse.getRate();
    if (rate == null || rate.getPrice() == null) {
      Log.error("missing 'price' in the GET /rate response");
      throw new ServerErrorException("internal server error");
    }

    if (request.getType() != GetRateRequest.Type.INDICATIVE_PRICES) {
      if (rate.getFee() == null || rate.getTotalPrice() == null) {
        Log.error("'fee' and/or 'total_price' are missing in the GET /rate response");
        throw new ServerErrorException("internal server error");
      }
    }

    if (request.getType() == GetRateRequest.Type.FIRM) {
      if (rate.getId() == null || rate.getExpiresAt() == null) {
        Log.error("'id' and/or 'expires_at' are missing in the GET /rate response");
        throw new ServerErrorException("internal server error");
      }
    }
    return getRateResponse;
  }

  String createJwtToken() {
    long issuedAt = Calendar.getInstance().getTimeInMillis() / 1000L;
    long expirationTime = issuedAt + (jwtExpirationMilliseconds / 1000L);
    JwtToken token = JwtToken.of(issuerUrl, issuedAt, expirationTime);
    return jwtService.encode(token);
  }
}

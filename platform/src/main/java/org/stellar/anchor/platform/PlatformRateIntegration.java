package org.stellar.anchor.platform;

import static okhttp3.HttpUrl.get;
import static org.stellar.anchor.platform.PlatformIntegrationHelper.*;

import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import okhttp3.*;
import okhttp3.HttpUrl.Builder;
import org.springframework.http.HttpStatus;
import org.stellar.anchor.exception.*;
import org.stellar.anchor.integration.rate.GetRateRequest;
import org.stellar.anchor.integration.rate.GetRateResponse;
import org.stellar.anchor.integration.rate.RateIntegration;
import shadow.com.google.common.reflect.TypeToken;

public class PlatformRateIntegration implements RateIntegration {
  private final String anchorEndpoint;
  private final OkHttpClient httpClient;

  public PlatformRateIntegration(String anchorEndpoint, OkHttpClient httpClient) {
    try {
      new URI(anchorEndpoint);
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException("invalid 'baseUri'");
    }

    this.anchorEndpoint = anchorEndpoint;
    this.httpClient = httpClient;
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
        new Request.Builder().url(url).header("Content-Type", "application/json").get().build();
    Response response = call(httpClient, httpRequest);
    String responseContent = getContent(response);

    if (response.code() != HttpStatus.OK.value()) {
      throw httpError(responseContent, response.code());
    }

    GetRateResponse getRateResponse;
    try {
      getRateResponse = gson.fromJson(responseContent, GetRateResponse.class);
    } catch (Exception e) { // cannot read body from response
      throw new ServerErrorException("internal server error", e);
    }

    GetRateResponse.Rate rate = getRateResponse.getRate();
    if (rate == null || rate.getPrice() == null) {
      throw new ServerErrorException("internal server error");
    }
    if (request.getType() == GetRateRequest.Type.FIRM) {
      if (rate.getId() == null || rate.getExpiresAt() == null) {
        throw new ServerErrorException("internal server error");
      }
    }
    return getRateResponse;
  }
}

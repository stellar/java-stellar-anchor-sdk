package org.stellar.anchor.platform;

import static okhttp3.HttpUrl.get;
import static org.stellar.anchor.platform.PlatformIntegrationHelper.*;

import com.google.gson.Gson;
import java.net.URI;
import java.net.URISyntaxException;
import okhttp3.*;
import okhttp3.HttpUrl.Builder;
import org.springframework.http.HttpStatus;
import org.stellar.anchor.exception.*;
import org.stellar.anchor.integration.rate.GetRateRequest;
import org.stellar.anchor.integration.rate.GetRateResponse;
import org.stellar.anchor.integration.rate.RateIntegration;

public class PlatformRateIntegration implements RateIntegration {
  private final String anchorEndpoint;
  private final OkHttpClient httpClient;
  private final Gson gson = new Gson();

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

    if (request.getType() != null) {
      urlBuilder.addQueryParameter("type", request.getType().toString());
    }
    if (request.getSellAsset() != null) {
      urlBuilder.addQueryParameter("sell_asset", request.getSellAsset());
    }
    if (request.getBuyAsset() != null) {
      urlBuilder.addQueryParameter("buy_asset", request.getBuyAsset());
    }
    if (request.getSellAmount() != null) {
      urlBuilder.addQueryParameter("sell_amount", request.getSellAmount());
    }
    if (request.getBuyAmount() != null) {
      urlBuilder.addQueryParameter("buy_amount", request.getBuyAmount());
    }
    if (request.getSellDeliveryMethod() != null) {
      urlBuilder.addQueryParameter("sell_delivery_method", request.getSellDeliveryMethod());
    }
    if (request.getBuyDeliveryMethod() != null) {
      urlBuilder.addQueryParameter("buy_delivery_method", request.getBuyDeliveryMethod());
    }
    if (request.getClientDomain() != null) {
      urlBuilder.addQueryParameter("client_domain", request.getClientDomain());
    }
    if (request.getAccount() != null) {
      urlBuilder.addQueryParameter("account", request.getAccount());
    }
    if (request.getMemo() != null) {
      urlBuilder.addQueryParameter("memo", request.getMemo());
    }
    if (request.getMemoType() != null) {
      urlBuilder.addQueryParameter("memo_type", request.getMemoType());
    }

    Request httpRequest =
        new Request.Builder()
            .url(urlBuilder.build())
            .header("Content-Type", "application/json")
            .get()
            .build();
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
    if (getRateResponse.getPrice() == null) {
      throw new ServerErrorException("internal server error");
    }
    return getRateResponse;
  }
}

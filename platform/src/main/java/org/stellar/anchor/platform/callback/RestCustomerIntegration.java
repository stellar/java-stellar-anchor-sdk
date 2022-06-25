package org.stellar.anchor.platform.callback;

import static okhttp3.HttpUrl.get;
import static org.stellar.anchor.platform.PlatformIntegrationHelper.*;
import static org.stellar.anchor.platform.callback.RestCustomerIntegration.Converter.fromPlatform;
import static org.stellar.anchor.platform.callback.RestCustomerIntegration.Converter.fromSep12;

import com.google.gson.Gson;
import java.net.URI;
import java.net.URISyntaxException;
import lombok.SneakyThrows;
import okhttp3.*;
import okhttp3.HttpUrl.Builder;
import org.springframework.http.HttpStatus;
import org.stellar.anchor.api.callback.*;
import org.stellar.anchor.api.callback.CustomerIntegration;
import org.stellar.anchor.api.exception.AnchorException;
import org.stellar.anchor.api.exception.ServerErrorException;
import org.stellar.anchor.api.sep.sep12.Sep12GetCustomerRequest;
import org.stellar.anchor.api.sep.sep12.Sep12GetCustomerResponse;
import org.stellar.anchor.api.sep.sep12.Sep12PutCustomerRequest;
import org.stellar.anchor.api.sep.sep12.Sep12PutCustomerResponse;

public class RestCustomerIntegration implements CustomerIntegration {
  private final String anchorEndpoint;
  private final OkHttpClient httpClient;
  private final AuthHelper authHelper;
  private final Gson gson;

  public RestCustomerIntegration(
      String anchorEndpoint, OkHttpClient httpClient, AuthHelper authHelper, Gson gson) {
    try {
      new URI(anchorEndpoint);
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException("invalid 'baseUri'");
    }

    this.anchorEndpoint = anchorEndpoint;
    this.httpClient = httpClient;
    this.authHelper = authHelper;
    this.gson = gson;
  }

  @Override
  public Sep12GetCustomerResponse getCustomer(Sep12GetCustomerRequest sep12GetCustomerRequest)
      throws AnchorException {
    GetCustomerRequest customerRequest = fromSep12(sep12GetCustomerRequest, gson);
    Builder customerEndpointBuilder = getCustomerUrlBuilder();
    if (customerRequest.getId() != null) {
      customerEndpointBuilder.addQueryParameter("id", customerRequest.getId());
    } else {
      customerEndpointBuilder.addQueryParameter("account", customerRequest.getAccount());
      if (customerRequest.getMemo() != null && customerRequest.getMemoType() != null) {
        customerEndpointBuilder
            .addQueryParameter("memo", customerRequest.getMemo())
            .addQueryParameter("memo_type", customerRequest.getMemoType());
      }
    }
    if (customerRequest.getType() != null) {
      customerEndpointBuilder.addQueryParameter("type", customerRequest.getType());
    }
    // Call anchor
    String authHeader = "Bearer " + authHelper.createJwtToken();
    Response response =
        call(
            httpClient,
            new Request.Builder()
                .url(customerEndpointBuilder.build())
                .header("Content-Type", "application/json")
                .header("Authorization", authHeader)
                .get()
                .build());
    String responseContent = getContent(response);

    if (response.code() == HttpStatus.OK.value()) {
      GetCustomerResponse getCustomerResponse;
      try {
        getCustomerResponse = gson.fromJson(responseContent, GetCustomerResponse.class);
      } catch (Exception e) { // cannot read body from response
        throw new ServerErrorException("internal server error", e);
      }
      if (getCustomerResponse.getStatus() == null) {
        throw new ServerErrorException("internal server error");
      }
      return fromPlatform(getCustomerResponse, gson);
    } else {
      throw httpError(responseContent, response.code(), gson);
    }
  }

  @Override
  public Sep12PutCustomerResponse putCustomer(Sep12PutCustomerRequest sep12PutCustomerRequest)
      throws AnchorException {
    PutCustomerRequest customerRequest = fromSep12(sep12PutCustomerRequest, gson);
    String authHeader = "Bearer " + authHelper.createJwtToken();
    RequestBody requestBody =
        RequestBody.create(gson.toJson(customerRequest), MediaType.get("application/json"));
    Request callbackRequest =
        new Request.Builder()
            .url(getCustomerUrlBuilder().build())
            .header("Authorization", authHeader)
            .put(requestBody)
            .build();

    // Call anchor
    Response response = call(httpClient, callbackRequest);
    String responseContent = getContent(response);

    if (response.code() == HttpStatus.OK.value()) {
      try {
        return fromPlatform(gson.fromJson(responseContent, PutCustomerResponse.class), gson);
      } catch (Exception e) {
        throw new ServerErrorException("internal server error", e);
      }
    } else {
      throw httpError(responseContent, response.code(), gson);
    }
  }

  @SneakyThrows
  @Override
  public void deleteCustomer(String id) {
    HttpUrl url = getCustomerUrlBuilder().addPathSegment(id).build();
    String authHeader = "Bearer " + authHelper.createJwtToken();
    Request callbackRequest =
        new Request.Builder()
            .url(url)
            .header("Content-Type", "application/json")
            .header("Authorization", authHeader)
            .delete()
            .build();

    // Call anchor
    Response response = call(httpClient, callbackRequest);
    String responseContent = getContent(response);

    if (response.code() != HttpStatus.NO_CONTENT.value()
        && response.code() != HttpStatus.OK.value()) {
      throw httpError(responseContent, response.code(), gson);
    }
  }

  @SneakyThrows
  @Override
  public PutCustomerVerificationResponse putVerification(PutCustomerVerificationRequest request) {
    // the Platform Callback API doesn't support verification.
    // if it does in the future we can implement this method
    throw new UnsupportedOperationException("not implemented");
  }

  Builder getCustomerUrlBuilder() {
    return get(anchorEndpoint).newBuilder().addPathSegment("customer");
  }

  static class Converter {
    public static Sep12GetCustomerResponse fromPlatform(GetCustomerResponse response, Gson gson) {
      String json = gson.toJson(response);
      return gson.fromJson(json, Sep12GetCustomerResponse.class);
    }

    public static Sep12PutCustomerResponse fromPlatform(PutCustomerResponse response, Gson gson) {
      String json = gson.toJson(response);
      return gson.fromJson(json, Sep12PutCustomerResponse.class);
    }

    public static GetCustomerRequest fromSep12(Sep12GetCustomerRequest request, Gson gson) {
      String json = gson.toJson(request);
      return gson.fromJson(json, GetCustomerRequest.class);
    }

    public static PutCustomerRequest fromSep12(Sep12PutCustomerRequest request, Gson gson) {
      String json = gson.toJson(request);
      return gson.fromJson(json, PutCustomerRequest.class);
    }
  }
}

package org.stellar.anchor.platform.callback;

import static okhttp3.HttpUrl.get;
import static org.stellar.anchor.platform.callback.PlatformIntegrationHelper.*;

import com.google.gson.Gson;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import lombok.SneakyThrows;
import okhttp3.*;
import okhttp3.HttpUrl.Builder;
import org.springframework.http.HttpStatus;
import org.stellar.anchor.api.callback.*;
import org.stellar.anchor.api.exception.AnchorException;
import org.stellar.anchor.api.exception.ServerErrorException;
import org.stellar.anchor.auth.AuthHelper;
import org.stellar.anchor.util.Log;
import shadow.com.google.common.reflect.TypeToken;

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
  public GetCustomerResponse getCustomer(GetCustomerRequest customerRequest)
      throws AnchorException {
    // prepare request
    Builder urlBuilder = getCustomerUrlBuilder();
    Type type = new TypeToken<Map<String, ?>>() {}.getType();
    Map<String, String> paramsMap = gson.fromJson(gson.toJson(customerRequest), type);
    paramsMap.forEach(
        (key, value) -> {
          if (value != null) {
            urlBuilder.addQueryParameter(key, value);
          }
        });
    HttpUrl url = urlBuilder.build();

    // Make request
    Response response = call(httpClient, getRequestBuilder(authHelper).url(url).get().build());
    String responseContent = getContent(response);

    if (response.code() != HttpStatus.OK.value()) {
      throw httpError(responseContent, response.code(), gson);
    }

    GetCustomerResponse getCustomerResponse;
    try {
      getCustomerResponse = gson.fromJson(responseContent, GetCustomerResponse.class);
    } catch (Exception e) { // cannot read body from response
      throw new ServerErrorException("internal server error", e);
    }

    if (getCustomerResponse.getStatus() == null) {
      Log.error("GET {callbackAPI}/customer response is missing the status field");
      throw new ServerErrorException(
          "internal server error: result from Anchor backend is invalid");
    }
    return getCustomerResponse;
  }

  @Override
  public PutCustomerResponse putCustomer(PutCustomerRequest putCustomerRequest)
      throws AnchorException {
    HttpUrl url = getCustomerUrlBuilder().build();

    RequestBody requestBody =
        RequestBody.create(gson.toJson(putCustomerRequest), MediaType.get("application/json"));
    Request callbackRequest = getRequestBuilder(authHelper).url(url).put(requestBody).build();

    // Call anchor
    Response response = call(httpClient, callbackRequest);
    String responseContent = getContent(response);

    if (!List.of(HttpStatus.OK.value(), HttpStatus.CREATED.value(), HttpStatus.ACCEPTED.value())
        .contains(response.code())) {
      throw httpError(responseContent, response.code(), gson);
    }

    try {
      return gson.fromJson(responseContent, PutCustomerResponse.class);
    } catch (Exception e) {
      throw new ServerErrorException("internal server error", e);
    }
  }

  @Override
  public void deleteCustomer(String id) throws AnchorException {
    HttpUrl url = getCustomerUrlBuilder().addPathSegment(id).build();
    Request callbackRequest = getRequestBuilder(authHelper).url(url).delete().build();

    // Call anchor
    Response response = call(httpClient, callbackRequest);
    String responseContent = getContent(response);

    if (!List.of(HttpStatus.OK.value(), HttpStatus.NO_CONTENT.value()).contains(response.code())) {
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
}

package org.stellar.anchor.platform.callback;

import static okhttp3.HttpUrl.get;
import static org.stellar.anchor.platform.callback.PlatformIntegrationHelper.*;
import static org.stellar.anchor.platform.callback.RestCustomerIntegration.Converter.*;

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
import org.stellar.anchor.api.callback.CustomerIntegration;
import org.stellar.anchor.api.exception.AnchorException;
import org.stellar.anchor.api.exception.ServerErrorException;
import org.stellar.anchor.api.sep.sep12.Sep12GetCustomerRequest;
import org.stellar.anchor.api.sep.sep12.Sep12GetCustomerResponse;
import org.stellar.anchor.api.sep.sep12.Sep12PutCustomerRequest;
import org.stellar.anchor.api.sep.sep12.Sep12PutCustomerResponse;
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
  public Sep12GetCustomerResponse getCustomer(Sep12GetCustomerRequest customerRequest)
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

    Sep12GetCustomerResponse getCustomerResponse;
    try {
      getCustomerResponse = sep12GetResponseFromCallbackApiBody(responseContent, gson);
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
  public Sep12PutCustomerResponse putCustomer(Sep12PutCustomerRequest sep12PutCustomerRequest)
      throws AnchorException {
    HttpUrl url = getCustomerUrlBuilder().build();
    RequestBody requestBody = requestBodyFromSep12Request(sep12PutCustomerRequest, gson);
    Request callbackRequest = getRequestBuilder(authHelper).url(url).put(requestBody).build();

    // Call anchor
    Response response = call(httpClient, callbackRequest);
    String responseContent = getContent(response);

    if (!List.of(HttpStatus.OK.value(), HttpStatus.CREATED.value(), HttpStatus.ACCEPTED.value())
        .contains(response.code())) {
      throw httpError(responseContent, response.code(), gson);
    }

    try {
      return sep12PutResponseFromCallbackApiBody(responseContent, gson);
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

  static class Converter {
    public static Sep12GetCustomerResponse sep12GetResponseFromCallbackApiBody(
        String body, Gson gson) {
      return gson.fromJson(body, Sep12GetCustomerResponse.class);
    }

    public static Sep12PutCustomerResponse sep12PutResponseFromCallbackApiBody(
        String body, Gson gson) {
      return gson.fromJson(body, Sep12PutCustomerResponse.class);
    }

    public static RequestBody requestBodyFromSep12Request(
        Sep12PutCustomerRequest request, Gson gson) {
      return RequestBody.create(gson.toJson(request), MediaType.get("application/json"));
    }
  }
}

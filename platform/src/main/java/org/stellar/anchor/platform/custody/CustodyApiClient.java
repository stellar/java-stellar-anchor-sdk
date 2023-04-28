package org.stellar.anchor.platform.custody;

import static org.stellar.anchor.util.OkHttpUtil.TYPE_JSON;

import com.google.gson.Gson;
import java.io.IOException;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.springframework.http.HttpStatus;
import org.stellar.anchor.api.custody.CreateCustodyTransactionRequest;
import org.stellar.anchor.api.custody.GenerateDepositAddressResponse;
import org.stellar.anchor.api.exception.CustodyException;
import org.stellar.anchor.auth.AuthHelper;
import org.stellar.anchor.platform.config.CustodyApiConfig;
import org.stellar.anchor.util.AuthHeader;
import org.stellar.anchor.util.GsonUtils;

public class CustodyApiClient {

  private static final Gson gson = GsonUtils.getInstance();

  private static final String CREATE_CUSTODY_TRANSACTION_URL_FORMAT = "/transactions/custody";
  private static final String GENERATE_DEPOSIT_ADDRESS_URL_FORMAT =
      "/transactions/payments/assets/%s/address";

  private final OkHttpClient httpClient;
  private final AuthHelper authHelper;
  private final CustodyApiConfig custodyApiConfig;

  public CustodyApiClient(
      OkHttpClient httpClient, AuthHelper authHelper, CustodyApiConfig custodyApiConfig) {
    this.httpClient = httpClient;
    this.authHelper = authHelper;
    this.custodyApiConfig = custodyApiConfig;
  }

  public void createCustodyTransaction(CreateCustodyTransactionRequest transactionRequest)
      throws CustodyException {
    Request request =
        getRequestBuilder()
            .url(custodyApiConfig.getBaseUrl() + CREATE_CUSTODY_TRANSACTION_URL_FORMAT)
            .post(RequestBody.create(gson.toJson(transactionRequest).getBytes(), TYPE_JSON))
            .build();
    doRequest(request);
  }

  public GenerateDepositAddressResponse generateDepositAddress(String assetId)
      throws CustodyException {
    Request request =
        getRequestBuilder()
            .url(
                custodyApiConfig.getBaseUrl()
                    + String.format(GENERATE_DEPOSIT_ADDRESS_URL_FORMAT, assetId))
            .get()
            .build();
    String responseBody = doRequest(request);
    return gson.fromJson(responseBody, GenerateDepositAddressResponse.class);
  }

  private Request.Builder getRequestBuilder() {
    Request.Builder requestBuilder = new Request.Builder();
    AuthHeader<String, String> authHeader = authHelper.createAuthHeader();
    return authHeader == null
        ? requestBuilder
        : requestBuilder.header(authHeader.getName(), authHeader.getValue());
  }

  private String doRequest(Request request) throws CustodyException {
    try (Response response = httpClient.newCall(request).execute()) {
      ResponseBody responseBody = response.body();
      String responseBodyJson = null;

      if (responseBody != null) {
        responseBodyJson = responseBody.string();
      }

      if (HttpStatus.valueOf(response.code()).is2xxSuccessful()) {
        return responseBodyJson;
      } else {
        throw new CustodyException(
            String.format(
                "Custody API returned an error. HTTP status[%d], response[%s]",
                response.code(), responseBodyJson));
      }
    } catch (IOException e) {
      throw new CustodyException("Exception occurred during request to Custody API", e);
    }
  }
}

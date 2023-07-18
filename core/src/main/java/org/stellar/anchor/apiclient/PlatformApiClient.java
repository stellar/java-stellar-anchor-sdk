package org.stellar.anchor.apiclient;

import static org.stellar.anchor.api.rpc.action.ActionMethod.NOTIFY_ONCHAIN_FUNDS_RECEIVED;
import static org.stellar.anchor.api.rpc.action.ActionMethod.NOTIFY_ONCHAIN_FUNDS_SENT;
import static org.stellar.anchor.api.rpc.action.ActionMethod.NOTIFY_TRANSACTION_ERROR;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.function.Function;
import javax.annotation.Nullable;
import okhttp3.HttpUrl;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.data.domain.Sort;
import org.stellar.anchor.api.exception.AnchorException;
import org.stellar.anchor.api.exception.InvalidConfigException;
import org.stellar.anchor.api.platform.*;
import org.stellar.anchor.api.rpc.RpcRequest;
import org.stellar.anchor.api.rpc.action.ActionMethod;
import org.stellar.anchor.api.rpc.action.AmountRequest;
import org.stellar.anchor.api.rpc.action.NotifyOnchainFundsReceivedRequest;
import org.stellar.anchor.api.rpc.action.NotifyOnchainFundsSentRequest;
import org.stellar.anchor.api.rpc.action.NotifyTransactionErrorRequest;
import org.stellar.anchor.api.sep.SepTransactionStatus;
import org.stellar.anchor.auth.AuthHelper;
import org.stellar.anchor.util.AuthHeader;
import org.stellar.anchor.util.OkHttpUtil;

public class PlatformApiClient extends BaseApiClient {
  private final AuthHelper authHelper;
  private final String endpoint;

  public static final String JSON_RPC_VERSION = "2.0";

  public PlatformApiClient(AuthHelper authHelper, String endpoint) {
    this.authHelper = authHelper;
    this.endpoint = endpoint;
  }

  public GetTransactionResponse getTransaction(String id) throws IOException, AnchorException {
    Request request = getRequestBuilder().url(endpoint + "/transactions/" + id).get().build();
    String responseBody = handleResponse(client.newCall(request).execute());
    return gson.fromJson(responseBody, GetTransactionResponse.class);
  }

  public GetTransactionsResponse getTransactions(
      TransactionsSeps sep,
      @Nullable TransactionsOrderBy order_by,
      @Nullable Sort.Direction order,
      @Nullable List<SepTransactionStatus> statuses,
      @Nullable Integer pageSize,
      @Nullable Integer pageNumber)
      throws IOException, AnchorException {
    HttpUrl.Builder builder = HttpUrl.parse(endpoint + "/transactions").newBuilder();

    builder.addQueryParameter("sep", sep.name().toLowerCase().replaceAll("sep_", ""));

    addToBuilder(builder, order_by, "order_by", x -> x.name().toLowerCase());
    addToBuilder(builder, order, "order", x -> x.name().toLowerCase());
    addToBuilder(builder, statuses, "statuses", SepTransactionStatus::mergeStatusesList);
    addToBuilder(builder, pageSize, "page_size", Object::toString);
    addToBuilder(builder, pageNumber, "page_number", Object::toString);

    Request request = getRequestBuilder().url(builder.build()).get().build();
    String responseBody = handleResponse(client.newCall(request).execute());
    return gson.fromJson(responseBody, GetTransactionsResponse.class);
  }

  private <T> void addToBuilder(
      HttpUrl.Builder builder, T val, String name, Function<T, String> f) {
    if (val != null) {
      builder.addQueryParameter(name, f.apply(val));
    }
  }

  public PatchTransactionsResponse patchTransaction(PatchTransactionsRequest txnRequest)
      throws IOException, AnchorException {
    HttpUrl url = HttpUrl.parse(endpoint);
    if (url == null)
      throw new InvalidConfigException(
          String.format("Invalid endpoint: %s of the client.", endpoint));
    url =
        new HttpUrl.Builder()
            .scheme(url.scheme())
            .host(url.host())
            .port(url.port())
            .addPathSegment("transactions")
            .build();

    RequestBody requestBody = OkHttpUtil.buildJsonRequestBody(gson.toJson(txnRequest));
    Request request = getRequestBuilder().url(url).patch(requestBody).build();
    Response response = client.newCall(request).execute();
    return gson.fromJson(handleResponse(response), PatchTransactionsResponse.class);
  }

  public void notifyOnchainFundsSent(String txnId, String stellarTxnId, String message)
      throws AnchorException, IOException {
    NotifyOnchainFundsSentRequest request =
        NotifyOnchainFundsSentRequest.builder()
            .transactionId(txnId)
            .stellarTransactionId(stellarTxnId)
            .message(message)
            .build();
    sendRpcNotification(NOTIFY_ONCHAIN_FUNDS_SENT, request);
  }

  public void notifyOnchainFundsReceived(
      String txnId, String stellarTxnId, String amountIn, String message)
      throws AnchorException, IOException {
    NotifyOnchainFundsReceivedRequest request =
        NotifyOnchainFundsReceivedRequest.builder()
            .transactionId(txnId)
            .stellarTransactionId(stellarTxnId)
            .message(message)
            .amountIn(new AmountRequest(amountIn))
            .build();
    sendRpcNotification(NOTIFY_ONCHAIN_FUNDS_RECEIVED, request);
  }

  public void notifyTransactionError(String txnId, String message)
      throws AnchorException, IOException {
    NotifyTransactionErrorRequest request =
        NotifyTransactionErrorRequest.builder().transactionId(txnId).message(message).build();
    sendRpcNotification(NOTIFY_TRANSACTION_ERROR, request);
  }

  public void sendRpcNotification(ActionMethod method, Object requestParams)
      throws IOException, AnchorException {
    RpcRequest rpcRequest =
        RpcRequest.builder()
            .method(method.toString())
            .jsonrpc(JSON_RPC_VERSION)
            .params(requestParams)
            .build();

    callRpcAction(List.of(rpcRequest));
  }

  public Response callRpcAction(List<RpcRequest> rpcRequests) throws IOException, AnchorException {
    HttpUrl url = HttpUrl.parse(endpoint);
    if (url == null)
      throw new InvalidConfigException(
          String.format("Invalid endpoint: %s of the client.", endpoint));
    url =
        new HttpUrl.Builder()
            .scheme(url.scheme())
            .host(url.host())
            .port(url.port())
            .addPathSegment("actions")
            .build();

    RequestBody requestBody = OkHttpUtil.buildJsonRequestBody(gson.toJson(rpcRequests));
    Request request = getRequestBuilder().url(url).post(requestBody).build();
    return client.newCall(request).execute();
  }

  public HashMap<?, ?> health(List<String> checks) throws IOException, AnchorException {
    HttpUrl url = HttpUrl.parse(endpoint);
    if (url == null)
      throw new InvalidConfigException(
          String.format("Invalid endpoint: %s of the client.", endpoint));
    HttpUrl.Builder builder =
        new HttpUrl.Builder()
            .scheme(url.scheme())
            .host(url.host())
            .port(url.port())
            .addPathSegment("health");
    if (checks.size() != 0) {
      String strChecks = String.join(",", checks);
      builder.addQueryParameter("checks", strChecks);
    }

    Request request =
        new Request.Builder()
            .url(endpoint + "/health")
            .header("Content-Type", "application/json")
            .get()
            .build();

    String responseBody = handleResponse(client.newCall(request).execute());
    return gson.fromJson(responseBody, HashMap.class);
  }

  Request.Builder getRequestBuilder() throws InvalidConfigException {
    Request.Builder requestBuilder =
        new Request.Builder().header("Content-Type", "application/json");

    AuthHeader<String, String> authHeader = authHelper.createPlatformServerAuthHeader();
    return authHeader == null
        ? requestBuilder
        : requestBuilder.header(authHeader.getName(), authHeader.getValue());
  }
}

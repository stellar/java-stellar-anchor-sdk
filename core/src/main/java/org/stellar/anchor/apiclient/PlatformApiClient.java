package org.stellar.anchor.apiclient;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
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
import org.stellar.anchor.api.sep.SepTransactionStatus;
import org.stellar.anchor.auth.AuthHelper;
import org.stellar.anchor.util.OkHttpUtil;

/** The client for the PlatformAPI endpoints. */
public class PlatformApiClient extends BaseApiClient {
  public PlatformApiClient(AuthHelper authHelper, String endpoint) {
    super(authHelper, endpoint);
  }

  /**
   * Get the transaction with the given id by calling the /transactions/{id} endpoint.
   *
   * @param id the id of the transaction to get.
   * @return the GetTransactionResponse.
   * @throws IOException if the request fails due to IO errors.
   * @throws AnchorException if the response is not successful.
   */
  public GetTransactionResponse getTransaction(String id) throws IOException, AnchorException {
    Request request = getRequestBuilder().url(endpoint + "/transactions/" + id).get().build();
    String responseBody = handleResponse(client.newCall(request).execute());
    return gson.fromJson(responseBody, GetTransactionResponse.class);
  }

  /**
   * Search the transactions with the given filters by calling the /transactions endpoint.
   *
   * @param sep The SEP number (eg: 6, 24, 31) to filter by.
   * @param order_by The field to order by.
   * @param order The direction to order by.
   * @param statuses The statuses to filter by.
   * @param pageSize The number of transactions to return per page.
   * @param pageNumber The page number of the search.
   * @return The GetTransactionsResponse.
   * @throws IOException if the request fails due to IO errors.
   * @throws AnchorException if the response is not successful.
   */
  public GetTransactionsResponse getTransactions(
      TransactionsSeps sep,
      @Nullable TransactionsOrderBy order_by,
      @Nullable Sort.Direction order,
      @Nullable List<SepTransactionStatus> statuses,
      @Nullable Integer pageSize,
      @Nullable Integer pageNumber)
      throws IOException, AnchorException {
    HttpUrl.Builder builder =
        Objects.requireNonNull(HttpUrl.parse(endpoint + "/transactions")).newBuilder();

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

  /**
   * Patch the transaction.
   *
   * @param txnRequest The request to patch the transaction.
   * @return The response of the patch request.
   * @throws IOException if the request fails due to IO errors.
   * @throws AnchorException if the response is not successful.
   */
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

  private <T> void addToBuilder(
      HttpUrl.Builder builder, T val, String name, Function<T, String> f) {
    if (val != null) {
      builder.addQueryParameter(name, f.apply(val));
    }
  }
}

package org.stellar.anchor.util;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import okhttp3.Response;
import org.stellar.anchor.api.exception.rpc.InvalidRequestException;
import org.stellar.anchor.api.rpc.RpcResponse;

public class RpcHelper {
  static final Gson gson = GsonUtils.getInstance();

  /**
   * Retrieves the result field from an RPC response.
   *
   * @param rpcResponse The Response object containing the JSON response body from an RPC call.
   * @return object populated with the data from the JSON response.
   * @throws IllegalArgumentException if the JSON response body does not contain any items in the
   *     list.
   */
  public static Object getResultFromRpcResponse(Response rpcResponse)
      throws InvalidRequestException, IOException {
    if (rpcResponse.body() == null) throw new InvalidRequestException("Empty response");
    String responseBody = rpcResponse.body().string();

    // List<RpcResponse> type token is needed due to rpcResponse is always wrapped in an array, e.g.
    // [{"jsonrpc":"2.0","result":{"id":"31c02..."}}]
    Type listType = new TypeToken<List<RpcResponse>>() {}.getType();
    List<RpcResponse> responseList = gson.fromJson(responseBody, listType);
    RpcResponse response = responseList.get(0);
    if (response.getError() != null) {
      throw new InvalidRequestException("Invalid JSON-RPC request");
    }
    return response.getResult();
  }
}

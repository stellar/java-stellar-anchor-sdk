package org.stellar.anchor.platform.utils;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.stellar.anchor.api.exception.RpcException;
import org.stellar.anchor.api.exception.rpc.InvalidRequestException;
import org.stellar.anchor.api.rpc.RpcRequest;
import org.stellar.anchor.api.rpc.RpcResponse;

public class RpcUtil {
  public static final String JSON_RPC_VERSION = "2.0";

  public static RpcResponse getRpcSuccessResponse(Object id, Object result) {
    return RpcResponse.builder().jsonrpc(JSON_RPC_VERSION).id(id).result(result).build();
  }

  public static RpcResponse getRpcErrorResponse(Object id, RpcException ex) {
    return RpcResponse.builder()
        .jsonrpc(JSON_RPC_VERSION)
        .id(id)
        .error(
            RpcResponse.RpcError.builder()
                .code(ex.getErrorCode())
                .message(ex.getMessage())
                .data(ex.getAdditionalData())
                .build())
        .build();
  }

  public static void validateRpcRequest(RpcRequest rpcRequest) throws InvalidRequestException {
    List<String> messages = new ArrayList<>();
    if (!JSON_RPC_VERSION.equals(rpcRequest.getJsonrpc())) {
      messages.add(
          String.format("Unsupported JSON-RPC protocol version [%s]", rpcRequest.getJsonrpc()));
    }

    String method = rpcRequest.getMethod();
    if (StringUtils.isBlank(method)) {
      messages.add("Method name can't be NULL or empty");
    }

    Object id = rpcRequest.getId();
    if (id != null) {
      if (!(id instanceof Number) && !(id instanceof String)) {
        messages.add("An identifier MUST contain a String, Number, or NULL value");
      }
    }

    if (!messages.isEmpty()) {
      throw new InvalidRequestException(String.join(";", messages));
    }
  }
}

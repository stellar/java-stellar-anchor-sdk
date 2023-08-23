package org.stellar.anchor.api.rpc;

import static java.util.function.UnaryOperator.identity;
import static java.util.stream.Collectors.toMap;

import java.util.Arrays;
import java.util.Map;

public enum RpcErrorCode {
  INVALID_REQUEST(-32600),
  METHOD_NOT_FOUND(-32601),
  INVALID_PARAMS(-32602),
  INTERNAL_ERROR(-32603);

  private final int errorCode;
  private static final Map<Integer, RpcErrorCode> errorCodeMap;

  static {
    errorCodeMap =
        Arrays.stream(RpcErrorCode.values()).collect(toMap(RpcErrorCode::getErrorCode, identity()));
  }

  RpcErrorCode(int errorCode) {
    this.errorCode = errorCode;
  }

  public int getErrorCode() {
    return errorCode;
  }

  public static RpcErrorCode findByErrorCode(int errorCode) {
    return errorCodeMap.get(errorCode);
  }
}

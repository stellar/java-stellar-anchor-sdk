package org.stellar.anchor.api.rpc;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public enum RpcErrorCode {
  INVALID_REQUEST(-32600),
  METHOD_NOT_FOUND(-32601),
  INVALID_PARAMS(-32602),
  INTERNAL_ERROR(-32603),
  PARSE_ERROR(-32700);

  private final int errorCode;
  private static final Map<Integer, RpcErrorCode> map;

  static {
    map =
        Arrays.stream(RpcErrorCode.values())
            .collect(Collectors.toMap(RpcErrorCode::getErrorCode, errorCode -> errorCode));
  }

  RpcErrorCode(int errorCode) {
    this.errorCode = errorCode;
  }

  public int getErrorCode() {
    return errorCode;
  }

  public static RpcErrorCode findByErrorCode(int errorCode) {
    return map.get(errorCode);
  }
}

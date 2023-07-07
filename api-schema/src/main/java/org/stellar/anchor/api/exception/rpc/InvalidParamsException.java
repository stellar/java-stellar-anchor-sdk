package org.stellar.anchor.api.exception.rpc;

import static org.stellar.anchor.api.rpc.RpcErrorCode.INVALID_PARAMS;

import lombok.EqualsAndHashCode;
import org.stellar.anchor.api.exception.RpcException;

/** Invalid method parameter(s). */
@EqualsAndHashCode(callSuper = false)
public class InvalidParamsException extends RpcException {
  public InvalidParamsException(String message) {
    super(INVALID_PARAMS, message);
  }
}

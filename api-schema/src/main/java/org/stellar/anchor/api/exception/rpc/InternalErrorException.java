package org.stellar.anchor.api.exception.rpc;

import static org.stellar.anchor.api.rpc.RpcErrorCode.INTERNAL_ERROR;

import lombok.EqualsAndHashCode;

/** Internal error occurred during rpc call processing. */
@EqualsAndHashCode(callSuper = false)
public class InternalErrorException extends RpcException {
  public InternalErrorException(String message) {
    super(INTERNAL_ERROR.getErrorCode(), message);
  }

  public InternalErrorException(String message, Throwable ex) {
    super(INTERNAL_ERROR.getErrorCode(), message, ex);
  }
}

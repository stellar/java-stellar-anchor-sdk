package org.stellar.anchor.api.exception.rpc;

import static org.stellar.anchor.api.rpc.RpcErrorCode.METHOD_NOT_FOUND;

import lombok.EqualsAndHashCode;

/** The method does not exist / is not available. */
@EqualsAndHashCode(callSuper = false)
public class MethodNotFoundException extends RpcException {
  public MethodNotFoundException(String message) {
    super(METHOD_NOT_FOUND.getErrorCode(), message);
  }
}

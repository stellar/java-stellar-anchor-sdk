package org.stellar.anchor.api.exception;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.stellar.anchor.api.rpc.RpcErrorCode;

@Getter
@EqualsAndHashCode(callSuper = false)
public class RpcException extends AnchorException {
  private final RpcErrorCode errorCode;
  private Object additionalData;

  public RpcException(RpcErrorCode errorCode, String message) {
    super(message);
    this.errorCode = errorCode;
  }

  public RpcException(RpcErrorCode errorCode, String message, Object additionalData) {
    super(message);
    this.errorCode = errorCode;
    this.additionalData = additionalData;
  }
}

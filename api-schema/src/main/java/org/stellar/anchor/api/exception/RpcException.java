package org.stellar.anchor.api.exception;

import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode(callSuper = false)
public class RpcException extends AnchorException {
  private final int errorCode;
  private Object additionalData;

  public RpcException(int errorCode, String message) {
    super(message);
    this.errorCode = errorCode;
  }

  public RpcException(int errorCode, String message, Object additionalData) {
    super(message);
    this.errorCode = errorCode;
    this.additionalData = additionalData;
  }
}

package org.stellar.anchor.api.exception;

import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = false)
public class UnprocessableEntityException extends AnchorException {
  public UnprocessableEntityException(String message) {
    super(message);
  }

  public UnprocessableEntityException(String message, Exception cause) {
    super(message, cause);
  }
}

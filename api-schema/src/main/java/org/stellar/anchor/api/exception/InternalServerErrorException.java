package org.stellar.anchor.api.exception;

import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = false)
public class InternalServerErrorException extends AnchorException {
  public InternalServerErrorException(String message, Exception cause) {
    super(message, cause);
  }

  public InternalServerErrorException(String message) {
    super(message);
  }
}

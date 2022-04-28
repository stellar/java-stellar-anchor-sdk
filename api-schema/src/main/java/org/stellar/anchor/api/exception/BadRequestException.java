package org.stellar.anchor.api.exception;

import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = false)
public class BadRequestException extends AnchorException {
  public BadRequestException(String message, Exception cause) {
    super(message, cause);
  }

  public BadRequestException(String message) {
    super(message);
  }
}

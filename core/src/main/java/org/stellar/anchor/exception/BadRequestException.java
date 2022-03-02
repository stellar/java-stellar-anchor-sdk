package org.stellar.anchor.exception;

public class BadRequestException extends AnchorException {
  public BadRequestException(String message, Exception cause) {
    super(message, cause);
  }

  public BadRequestException(String message) {
    super(message);
  }
}

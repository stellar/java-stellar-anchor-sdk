package org.stellar.anchor.api.exception;

public class ValueValidationException extends AnchorException {
  public ValueValidationException(String message, Exception cause) {
    super(message, cause);
  }

  public ValueValidationException(String message) {
    super(message);
  }
}

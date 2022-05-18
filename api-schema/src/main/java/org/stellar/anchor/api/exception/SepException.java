package org.stellar.anchor.api.exception;

public class SepException extends AnchorException {
  public SepException(String message, Exception cause) {
    super(message, cause);
  }

  public SepException(String message) {
    super(message);
  }
}

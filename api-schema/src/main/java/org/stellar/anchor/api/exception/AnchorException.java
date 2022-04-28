package org.stellar.anchor.api.exception;

public abstract class AnchorException extends Exception {
  public AnchorException(String message, Exception cause) {
    super(message, cause);
  }

  public AnchorException(String message) {
    super(message);
  }

  public AnchorException() {}
}

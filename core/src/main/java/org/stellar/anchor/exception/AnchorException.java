package org.stellar.anchor.exception;

public abstract class AnchorException extends Exception {
  public AnchorException(String message, Exception cause) {
    super(message, cause);
  }

  public AnchorException(String message) {
    super(message);
  }
}

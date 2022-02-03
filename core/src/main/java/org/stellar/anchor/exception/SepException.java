package org.stellar.anchor.exception;

public class SepException extends Exception {
  public SepException(String message, Exception cause) {
    super(message, cause);
  }

  public SepException(String message) {
    super(message);
  }
}

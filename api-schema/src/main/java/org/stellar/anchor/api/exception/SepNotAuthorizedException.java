package org.stellar.anchor.api.exception;

public class SepNotAuthorizedException extends SepException {
  public SepNotAuthorizedException(String message, Exception cause) {
    super(message, cause);
  }

  public SepNotAuthorizedException(String message) {
    super(message);
  }
}

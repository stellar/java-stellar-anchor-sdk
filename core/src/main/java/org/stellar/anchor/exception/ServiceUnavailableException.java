package org.stellar.anchor.exception;

public class ServiceUnavailableException extends AnchorException {
  public ServiceUnavailableException(String message, Exception cause) {
    super(message, cause);
  }

  public ServiceUnavailableException(String message) {
    super(message);
  }
}

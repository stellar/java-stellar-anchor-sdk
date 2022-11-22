package org.stellar.anchor.api.exception;

public class ServiceUnavailableException extends AnchorException {
  public ServiceUnavailableException(String message, Exception cause) {
    super(message, cause);
  }
}

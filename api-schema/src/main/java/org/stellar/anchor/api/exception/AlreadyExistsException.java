package org.stellar.anchor.api.exception;

public class AlreadyExistsException extends AnchorException {
  public AlreadyExistsException(String message, Exception cause) {
    super(message, cause);
  }

  public AlreadyExistsException(String message) {
    super(message);
  }
}

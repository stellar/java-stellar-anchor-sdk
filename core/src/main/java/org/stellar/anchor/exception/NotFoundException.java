package org.stellar.anchor.exception;

public class NotFoundException extends AnchorException {
  public NotFoundException(String message) {
    super(404, message);
  }
}

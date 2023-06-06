package org.stellar.anchor.api.exception;

/** Thrown when a specific operation had not been implemented. */
public class NotSupportedException extends AnchorException {
  public NotSupportedException(String message) {
    super(message);
  }
}

package org.stellar.anchor.api.exception;

/** Base class for all Anchor exceptions. */
public abstract class AnchorException extends Exception {
  public AnchorException(String message, Exception cause) {
    super(message, cause);
  }

  public AnchorException(String message) {
    super(message);
  }

  public AnchorException() {}
}

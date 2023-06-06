package org.stellar.anchor.api.exception;

/** Base class of SEP related exceptions. */
public class SepException extends AnchorException {
  public SepException(String message, Exception cause) {
    super(message, cause);
  }

  public SepException(String message) {
    super(message);
  }
}

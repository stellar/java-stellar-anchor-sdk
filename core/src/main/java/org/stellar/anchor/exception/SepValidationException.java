package org.stellar.anchor.exception;

/** The exception thrown by the SEP controller validation process. */
public class SepValidationException extends SepException {
  public SepValidationException(String message) {
    super(message);
  }

  public SepValidationException(String message, Exception cause) {
    super(message, cause);
  }

  public SepValidationException(int httpStatus, String message) {
    super(httpStatus, message);
  }
}

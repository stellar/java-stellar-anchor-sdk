package org.stellar.anchor.api.exception;

/**
 * Thrown when a SEP request failed to pass the validation checks.
 *
 * <p>This exception is thrown when the request is invalid and the server is not able to process it.
 * For example, when a SEP-24 request is missing a required parameter.
 */
public class SepValidationException extends SepException {
  public SepValidationException(String message, Exception cause) {
    super(message, cause);
  }

  public SepValidationException(String message) {
    super(message);
  }
}

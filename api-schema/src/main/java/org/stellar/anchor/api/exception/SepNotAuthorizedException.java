package org.stellar.anchor.api.exception;

/**
 * Thrown when a SEP related operation is not authorized.
 *
 * <p>This exception is thrown when a SEP related operation is not authorized. For example, when a
 * wallet client tries to send a SEP-24 transaction without having the required SEP-10
 * authorization.
 */
public class SepNotAuthorizedException extends SepException {
  public SepNotAuthorizedException(String message, Exception cause) {
    super(message, cause);
  }

  public SepNotAuthorizedException(String message) {
    super(message);
  }
}

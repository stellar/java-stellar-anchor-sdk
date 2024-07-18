package org.stellar.anchor.api.exception;

import lombok.EqualsAndHashCode;

/** Thrown when the client request is unauthorized. */
@EqualsAndHashCode(callSuper = false)
public class UnauthorizedException extends AnchorException {
  public UnauthorizedException(String message, Exception cause) {
    super(message, cause);
  }

  public UnauthorizedException(String message) {
    super(message);
  }
}

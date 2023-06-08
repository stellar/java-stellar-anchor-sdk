package org.stellar.anchor.api.exception;

import lombok.EqualsAndHashCode;

/** Thrown when a data entity is not able to be processed by the server. */
@EqualsAndHashCode(callSuper = false)
public class UnprocessableEntityException extends AnchorException {
  public UnprocessableEntityException(String message) {
    super(message);
  }

  public UnprocessableEntityException(String message, Exception cause) {
    super(message, cause);
  }
}

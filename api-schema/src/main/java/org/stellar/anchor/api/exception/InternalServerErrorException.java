package org.stellar.anchor.api.exception;

import lombok.EqualsAndHashCode;

/** Thrown when an internal server error occurs. */
@EqualsAndHashCode(callSuper = false)
public class InternalServerErrorException extends AnchorException {
  public InternalServerErrorException(String message) {
    super(message);
  }
}

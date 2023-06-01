package org.stellar.anchor.api.exception;

import lombok.EqualsAndHashCode;

/** Thrown when a requested resource is not found. */
@EqualsAndHashCode(callSuper = false)
public class NotFoundException extends AnchorException {
  public NotFoundException(String message) {
    super(message);
  }
}

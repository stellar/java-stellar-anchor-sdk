package org.stellar.anchor.exception;

import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = false)
public class NotFoundException extends AnchorException {
  public NotFoundException(String message) {
    super(message);
  }
}

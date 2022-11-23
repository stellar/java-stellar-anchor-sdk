package org.stellar.anchor.api.exception;

import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = false)
public class InternalServerErrorException extends AnchorException {
  public InternalServerErrorException(String message) {
    super(message);
  }
}

package org.stellar.anchor.exception;

import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = false)
public class UnprocessableEntityException extends AnchorException {
  public UnprocessableEntityException(String message) {
    super(message);
  }
}

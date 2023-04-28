package org.stellar.anchor.api.exception;

import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = false)
public class CustodyException extends AnchorException {

  public CustodyException(String message, Exception cause) {
    super(message, cause);
  }

  public CustodyException(String message) {
    super(message);
  }
}

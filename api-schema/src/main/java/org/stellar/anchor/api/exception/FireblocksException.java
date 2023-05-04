package org.stellar.anchor.api.exception;

import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = false)
public class FireblocksException extends CustodyException {

  public FireblocksException(String message, Exception cause) {
    super(message, cause);
  }

  public FireblocksException(String message) {
    super(message);
  }
}

package org.stellar.anchor.platform.exception;

import lombok.EqualsAndHashCode;
import org.stellar.anchor.api.exception.AnchorException;

@EqualsAndHashCode(callSuper = false)
public class CustodyException extends AnchorException {

  public CustodyException(String message, Exception cause) {
    super(message, cause);
  }

  public CustodyException(String message) {
    super(message);
  }
}

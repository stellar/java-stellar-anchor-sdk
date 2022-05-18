package org.stellar.anchor.api.exception;

import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = false)
public class ServerErrorException extends AnchorException {
  public ServerErrorException(String message, Exception cause) {
    super(message, cause);
  }

  public ServerErrorException(String message) {
    super(message);
  }
}

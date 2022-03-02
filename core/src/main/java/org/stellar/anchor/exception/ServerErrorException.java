package org.stellar.anchor.exception;

public class ServerErrorException extends AnchorException {
  public ServerErrorException(String message, Exception cause) {
    super(message, cause);
  }

  public ServerErrorException(String message) {
    super(message);
  }
}

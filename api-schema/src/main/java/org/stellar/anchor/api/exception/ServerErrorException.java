package org.stellar.anchor.api.exception;

import lombok.EqualsAndHashCode;

/**
 * Thrown when a backend server request error occurs.
 *
 * <p>This exception is thrown when the server is not able to process the request due to an internal
 * error. For example, when the server is not able to connect to the anchor business server.
 */
@EqualsAndHashCode(callSuper = false)
public class ServerErrorException extends AnchorException {
  public ServerErrorException(String message, Exception cause) {
    super(message, cause);
  }

  public ServerErrorException(String message) {
    super(message);
  }
}

package org.stellar.anchor.exception;

import org.apache.http.HttpStatus;

public class AnchorException extends RuntimeException {
  int httpStatus = HttpStatus.SC_INTERNAL_SERVER_ERROR;

  public AnchorException(String message, Exception cause) {
    super(message, cause);
  }

  public AnchorException(String message) {
    super(message);
  }

  public AnchorException(int httpStatus, String message) {
    this(message);
    this.httpStatus = httpStatus;
  }

  int getHttpStatus() {
    return httpStatus;
  }
}

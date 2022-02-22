package org.stellar.anchor.exception;

import org.apache.http.HttpStatus;

public class SepException extends Exception {
  int httpStatus = HttpStatus.SC_INTERNAL_SERVER_ERROR;

  public SepException(String message, Exception cause) {
    super(message, cause);
  }

  public SepException(String message) {
    super(message);
  }

  public SepException(int httpStatus, String message) {
    this(message);
    this.httpStatus = httpStatus;
  }

  int getHttpStatus() {
    return httpStatus;
  }
}

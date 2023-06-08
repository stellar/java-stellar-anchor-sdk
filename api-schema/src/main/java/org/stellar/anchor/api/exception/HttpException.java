package org.stellar.anchor.api.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;
import reactor.util.annotation.Nullable;

/** Thrown when an HTTP error occurs to wrap the original status code and errors. */
@Data
@EqualsAndHashCode(callSuper = false)
public class HttpException extends RuntimeException {
  final int statusCode;
  final String reason;
  final String internalCode;

  public HttpException(int statusCode) {
    this(statusCode, null, null, null);
  }

  public HttpException(
      int statusCode,
      @Nullable String reason,
      @Nullable String internalCode,
      @Nullable Throwable cause) {
    super(reason, cause);
    this.statusCode = statusCode;
    this.reason = reason;
    this.internalCode = internalCode;
  }
}

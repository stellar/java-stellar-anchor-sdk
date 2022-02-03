package org.stellar.anchor.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;
import reactor.util.annotation.Nullable;

@Data
@EqualsAndHashCode(callSuper = false)
public class HttpException extends RuntimeException {
  final int statusCode;
  final String reason;
  final String internalCode;

  public HttpException(int statusCode) {
    this(statusCode, null, null, null);
  }

  public HttpException(int statusCode, @Nullable String reason) {
    this(statusCode, reason, null, null);
  }

  public HttpException(int statusCode, @Nullable String reason, @Nullable String internalCode) {
    this(statusCode, reason, internalCode, null);
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

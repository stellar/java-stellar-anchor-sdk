package org.stellar.anchor.platform.controller;

import static org.stellar.anchor.util.Log.errorEx;

import java.io.IOException;
import javax.transaction.NotSupportedException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.stellar.anchor.dto.SepExceptionResponse;
import org.stellar.anchor.exception.NotFoundException;
import org.stellar.anchor.exception.SepNotAuthorizedException;
import org.stellar.anchor.exception.SepNotFoundException;
import org.stellar.anchor.exception.SepValidationException;

/** The uncaught exception handler. */
@RestControllerAdvice
public class GlobalControllerExceptionHandler {
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  @ExceptionHandler({SepValidationException.class})
  public SepExceptionResponse handleBadRequest(SepValidationException ex) {
    errorEx(ex);
    return new SepExceptionResponse(ex.getMessage());
  }

  @ResponseStatus(HttpStatus.FORBIDDEN)
  @ExceptionHandler({SepNotAuthorizedException.class})
  public SepExceptionResponse handleAuthError(SepValidationException ex) {
    errorEx(ex);
    return new SepExceptionResponse(ex.getMessage());
  }

  @ExceptionHandler({SepNotFoundException.class, NotFoundException.class})
  @ResponseStatus(value = HttpStatus.NOT_FOUND)
  SepExceptionResponse handleNotFound(Exception ex) {
    errorEx(ex);
    return new SepExceptionResponse(ex.getMessage());
  }

  @ResponseStatus(HttpStatus.NOT_IMPLEMENTED)
  @ExceptionHandler({NotSupportedException.class})
  public SepExceptionResponse handleNotImplementedError(Exception ex) {
    errorEx(ex);
    return new SepExceptionResponse(ex.getMessage());
  }

  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  @ExceptionHandler({IOException.class, NullPointerException.class, RuntimeException.class})
  public SepExceptionResponse handleInternalError(Exception ex) {
    errorEx(ex);
    return new SepExceptionResponse(ex.getMessage());
  }
}

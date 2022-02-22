package org.stellar.anchor.reference.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.stellar.anchor.dto.SepExceptionResponse;
import org.stellar.anchor.exception.SepNotFoundException;
import org.stellar.anchor.exception.SepValidationException;

import java.io.IOException;

import static org.stellar.anchor.util.Log.errorEx;

@RestControllerAdvice
public class GlobalControllerExceptionHandler {
  @ResponseStatus(HttpStatus.FORBIDDEN)
  @ExceptionHandler({SepValidationException.class})
  public SepExceptionResponse handleAuthError(SepValidationException ex) {
    errorEx(ex);
    return new SepExceptionResponse(ex.getMessage());
  }

  @ResponseStatus(HttpStatus.NOT_FOUND)
  @ExceptionHandler({SepNotFoundException.class})
  public SepExceptionResponse handleNotFoundError(Exception ex) {
    errorEx(ex);
    return new SepExceptionResponse(ex.getMessage());
  }

  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  @ExceptionHandler({IOException.class, NullPointerException.class})
  public SepExceptionResponse handleInternalError(Exception ex) {
    errorEx(ex);
    return new SepExceptionResponse(ex.getMessage());
  }
}

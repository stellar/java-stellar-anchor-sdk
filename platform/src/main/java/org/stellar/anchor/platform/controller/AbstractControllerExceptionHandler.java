package org.stellar.anchor.platform.controller;

import static org.stellar.anchor.util.Log.errorEx;

import javax.transaction.NotSupportedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.stellar.anchor.api.exception.*;
import org.stellar.anchor.api.sep.SepExceptionResponse;

public abstract class AbstractControllerExceptionHandler {
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  @ExceptionHandler({SepValidationException.class, BadRequestException.class})
  public SepExceptionResponse handleBadRequest(AnchorException ex) {
    errorEx(ex);
    return new SepExceptionResponse(ex.getMessage());
  }

  @ResponseStatus(HttpStatus.BAD_REQUEST)
  @ExceptionHandler(MissingServletRequestParameterException.class)
  public SepExceptionResponse handleMissingParams(MissingServletRequestParameterException ex) {
    errorEx(ex);
    String name = ex.getParameterName();
    return new SepExceptionResponse(String.format("The \"%s\" parameter is missing.", name));
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  @ResponseStatus(value = HttpStatus.BAD_REQUEST)
  public SepExceptionResponse handleRandomException(HttpMessageNotReadableException ex) {
    errorEx(ex);
    return new SepExceptionResponse("Your request body is wrong in some way.");
  }

  @ResponseStatus(HttpStatus.FORBIDDEN)
  @ExceptionHandler({SepNotAuthorizedException.class})
  public SepExceptionResponse handleAuthError(SepException ex) {
    errorEx(ex);
    return new SepExceptionResponse(ex.getMessage());
  }

  @ExceptionHandler({SepNotFoundException.class, NotFoundException.class})
  @ResponseStatus(value = HttpStatus.NOT_FOUND)
  SepExceptionResponse handleNotFound(AnchorException ex) {
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
  @ExceptionHandler({Exception.class})
  public SepExceptionResponse handleInternalError(Exception ex) {
    errorEx(ex);
    return new SepExceptionResponse(ex.getMessage());
  }
}

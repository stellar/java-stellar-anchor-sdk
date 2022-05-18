package org.stellar.anchor.reference.controller;

import static org.stellar.anchor.util.Log.errorEx;

import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.stellar.anchor.api.exception.*;
import org.stellar.anchor.api.sep.SepExceptionResponse;

@RestControllerAdvice
public class GlobalControllerExceptionHandler {
  @ResponseStatus(HttpStatus.NOT_FOUND)
  @ExceptionHandler({SepNotFoundException.class, NotFoundException.class})
  public SepExceptionResponse handleNotFoundError(AnchorException ex) {
    errorEx(ex);
    return new SepExceptionResponse(ex.getMessage());
  }

  @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
  @ExceptionHandler({BadRequestException.class, UnprocessableEntityException.class})
  public SepExceptionResponse handleUnprocessableEntityError(AnchorException ex) {
    errorEx(ex);
    return new SepExceptionResponse(ex.getMessage());
  }

  @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
  @ExceptionHandler(MissingServletRequestParameterException.class)
  public SepExceptionResponse handleMissingParams(MissingServletRequestParameterException ex) {
    errorEx(ex);
    String name = ex.getParameterName();
    return new SepExceptionResponse(String.format("The \"%s\" parameter is missing.", name));
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  @ResponseStatus(value = HttpStatus.UNPROCESSABLE_ENTITY)
  public SepExceptionResponse handleRandomException(HttpMessageNotReadableException ex) {
    errorEx(ex);
    return new SepExceptionResponse("Your request body is wrong in some way.");
  }

  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  @ExceptionHandler({
    IOException.class,
    NullPointerException.class,
    RuntimeException.class,
    ServerErrorException.class,
    Exception.class
  })
  public SepExceptionResponse handleInternalError(Exception ex) {
    errorEx(ex);
    return new SepExceptionResponse(ex.getMessage());
  }
}

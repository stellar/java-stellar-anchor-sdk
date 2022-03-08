package org.stellar.anchor.platform.controller;

import static org.stellar.anchor.util.Log.*;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientException;
import org.stellar.anchor.dto.SepExceptionResponse;
import org.stellar.anchor.dto.sep38.InfoResponse;
import org.stellar.anchor.sep38.Sep38Service;

@RestController
@RequestMapping("/sep38")
public class Sep38Controller {
  private final Sep38Service sep38Service;

  public Sep38Controller(Sep38Service sep38Service) {
    this.sep38Service = sep38Service;
  }

  @CrossOrigin(origins = "*")
  @RequestMapping(
      value = "/info",
      method = {RequestMethod.GET})
  public InfoResponse getInfo() {
    // TODO: add integration tests for `GET /info`
    return sep38Service.getInfo();
  }

  @ExceptionHandler(RestClientException.class)
  @ResponseStatus(value = HttpStatus.SERVICE_UNAVAILABLE)
  public SepExceptionResponse handleRestClientException(RestClientException ex) {
    errorEx(ex);
    return new SepExceptionResponse(ex.getMessage());
  }
}

package org.stellar.anchor.platform.controller;

import java.io.IOException;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.stellar.anchor.api.exception.SepNotFoundException;
import org.stellar.anchor.api.sep.SepExceptionResponse;
import org.stellar.anchor.config.Sep1Config;
import org.stellar.anchor.platform.condition.ConditionalOnAllSepsEnabled;
import org.stellar.anchor.sep1.Sep1Service;

@RestController
@CrossOrigin(origins = "*")
@ConditionalOnAllSepsEnabled(seps = {"sep1"})
@Profile("default")
public class Sep1Controller {
  private final Sep1Config sep1Config;
  private final Sep1Service sep1Service;

  public Sep1Controller(Sep1Config sep1Config, Sep1Service sep1Service) {
    this.sep1Config = sep1Config;
    this.sep1Service = sep1Service;
  }

  @CrossOrigin(origins = "*")
  @RequestMapping(
      value = "/.well-known/stellar.toml",
      method = {RequestMethod.GET, RequestMethod.OPTIONS})
  public ResponseEntity<String> getToml() throws IOException, SepNotFoundException {
    if (!sep1Config.isEnabled()) {
      throw new SepNotFoundException("Not Found");
    }
    HttpHeaders headers = new HttpHeaders();
    headers.set("content-type", "text/plain");
    return ResponseEntity.ok().headers(headers).body(sep1Service.getStellarToml());
  }

  @ExceptionHandler({SepNotFoundException.class})
  @ResponseStatus(value = HttpStatus.NOT_FOUND)
  SepExceptionResponse handleNotFound(Exception ex) {
    return new SepExceptionResponse(ex.getMessage());
  }
}

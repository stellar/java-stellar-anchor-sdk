package org.stellar.anchor.reference.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.stellar.anchor.util.HealthCheck;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping(value = "/health")
public class HealthController {
  @RequestMapping(method = {RequestMethod.GET})
  public HealthCheck health() {
    return new HealthCheck(true);
  }
}

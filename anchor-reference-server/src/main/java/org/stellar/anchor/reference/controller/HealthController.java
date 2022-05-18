package org.stellar.anchor.reference.controller;

import java.util.List;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.stellar.anchor.api.platform.HealthCheckResponse;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping(value = "/health")
public class HealthController {
  @RequestMapping(method = {RequestMethod.GET})
  public HealthCheckResponse health() {
    HealthCheckResponse healthCheckResponse = new HealthCheckResponse();
    return healthCheckResponse.complete(List.of());
  }
}

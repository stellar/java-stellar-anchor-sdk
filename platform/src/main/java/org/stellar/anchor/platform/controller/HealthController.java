package org.stellar.anchor.platform.controller;

import java.util.List;
import org.springframework.web.bind.annotation.*;
import org.stellar.anchor.api.platform.HealthCheckResponse;
import org.stellar.anchor.platform.service.HealthCheckService;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping(value = "/health")
public class HealthController {
  HealthCheckService healthCheckService;

  HealthController(HealthCheckService healthCheckService) {
    this.healthCheckService = healthCheckService;
  }

  @RequestMapping(method = {RequestMethod.GET})
  public HealthCheckResponse health(@RequestParam(required = false) List<String> checks) {
    if (checks == null) {
      checks = List.of("all");
    }
    return healthCheckService.check(checks);
  }
}

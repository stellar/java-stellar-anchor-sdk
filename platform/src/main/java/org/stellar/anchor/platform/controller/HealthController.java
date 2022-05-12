package org.stellar.anchor.platform.controller;

import org.springframework.web.bind.annotation.*;
import org.stellar.anchor.api.platform.HealthCheck;
import org.stellar.anchor.platform.service.HealthCheckService;

import java.util.List;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping(value = "/health")
public class HealthController {
  HealthCheckService healthCheckService;

  HealthController(HealthCheckService healthCheckService) {
    this.healthCheckService = healthCheckService;
  }
  @RequestMapping(method = {RequestMethod.GET})
  public HealthCheck health(@RequestParam List<String> checks) {
    return healthCheckService.check(checks);
  }
}

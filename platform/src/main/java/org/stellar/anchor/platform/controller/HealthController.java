package org.stellar.anchor.platform.controller;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.stellar.anchor.api.platform.HealthCheckResponse;
import org.stellar.anchor.api.platform.HealthCheckStatus;
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
  public ResponseEntity<HealthCheckResponse> health(
      @RequestParam(required = false) List<String> checks) {
    if (checks == null) {
      checks = List.of("all");
    }
    HealthCheckResponse healthCheckResponse = healthCheckService.check(checks);

    boolean unhealthy =
        healthCheckResponse.getChecks().values().stream()
            .anyMatch(result -> result.getStatus() == HealthCheckStatus.RED);
    if (unhealthy) {
      return new ResponseEntity<>(healthCheckResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    } else {
      return ResponseEntity.ok(healthCheckResponse);
    }
  }
}

package org.stellar.anchor.platform.controller;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.stellar.anchor.api.platform.HealthCheckResponse;
import org.stellar.anchor.api.platform.HealthCheckStatus;
import org.stellar.anchor.platform.service.HealthCheckService;

public abstract class HealthController {
  final HealthCheckService healthCheckService;

  protected HealthController(HealthCheckService healthCheckService) {
    this.healthCheckService = healthCheckService;
  }

  @RequestMapping(
      method = {RequestMethod.GET},
      produces = {MediaType.APPLICATION_JSON_VALUE})
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

package org.stellar.anchor.reference.controller;

import java.util.List;
import org.springframework.web.bind.annotation.*;
import org.stellar.anchor.api.platform.HealthCheckResponse;
import org.stellar.anchor.reference.service.HealthCheckService;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping(value = "/health")
public class HealthController {
  HealthCheckService healthCheckService;

  HealthController(HealthCheckService healthCheckService) {
    this.healthCheckService = healthCheckService;
  }

  /**
   * Perform health checks. Example:
   *
   * <pre>
   * {
   *    "started": "2022-05-23T13:15:21.909769800Z",
   *    "elapsed_time_ms": 0,
   *    "number_of_checks": 1,
   *    "checks": {
   *      "kafka_listener": {
   *        "status": "green",
   *        "running": true,
   *        "kafka_available": true
   *      }
   *    }
   *  }
   *  </pre>
   */
  @RequestMapping(method = {RequestMethod.GET})
  public HealthCheckResponse health(@RequestParam(required = false) List<String> checks) {
    if (checks == null) {
      checks = List.of("all");
    }
    return healthCheckService.check(checks);
  }
}

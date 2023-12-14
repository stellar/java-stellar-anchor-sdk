package org.stellar.anchor.platform.controller.platform;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.stellar.anchor.platform.controller.HealthController;
import org.stellar.anchor.platform.service.HealthCheckService;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping(
    value = "/health",
    produces = {MediaType.APPLICATION_JSON_VALUE})
public class PlatformHealthController extends HealthController {
  public PlatformHealthController(HealthCheckService healthCheckService) {
    super(healthCheckService);
  }
}

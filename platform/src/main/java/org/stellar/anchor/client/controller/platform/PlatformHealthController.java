package org.stellar.anchor.client.controller.platform;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.stellar.anchor.client.controller.HealthController;
import org.stellar.anchor.client.service.HealthCheckService;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping(value = "/health")
public class PlatformHealthController extends HealthController {
  public PlatformHealthController(HealthCheckService healthCheckService) {
    super(healthCheckService);
  }
}

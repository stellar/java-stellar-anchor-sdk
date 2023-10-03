package org.stellar.anchor.platform.controller.custody;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.stellar.anchor.platform.controller.HealthController;
import org.stellar.anchor.platform.service.HealthCheckService;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping(value = "/health")
public class CustodyHealthController extends HealthController {
  public CustodyHealthController(HealthCheckService healthCheckService) {
    super(healthCheckService);
  }
}

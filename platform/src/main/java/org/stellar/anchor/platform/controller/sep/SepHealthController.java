package org.stellar.anchor.platform.controller.sep;

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
public class SepHealthController extends HealthController {
  public SepHealthController(HealthCheckService healthCheckService) {
    super(healthCheckService);
  }
}

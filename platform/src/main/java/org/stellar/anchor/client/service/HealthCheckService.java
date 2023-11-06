package org.stellar.anchor.client.service;

import static org.stellar.anchor.util.Log.debugF;
import static org.stellar.anchor.util.Log.warnF;

import java.util.List;
import org.stellar.anchor.api.platform.HealthCheckResponse;
import org.stellar.anchor.healthcheck.HealthCheckProcessor;
import org.stellar.anchor.healthcheck.HealthCheckable;

public class HealthCheckService {
  final HealthCheckProcessor processor;

  public HealthCheckService(List<HealthCheckable> checkables) {
    checkables.forEach(
        checkable -> debugF("{} is added to the health check list.", checkable.getName()));
    if (checkables.size() == 0) {
      warnF("No health-checkable services are found");
    }
    processor = new HealthCheckProcessor(checkables);
  }

  public HealthCheckResponse check(List<String> checks) {
    return processor.check(checks);
  }
}

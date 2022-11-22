package org.stellar.anchor.reference.service;

import static org.stellar.anchor.util.Log.*;

import java.util.List;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;
import org.stellar.anchor.api.platform.HealthCheckResponse;
import org.stellar.anchor.healthcheck.HealthCheckProcessor;
import org.stellar.anchor.healthcheck.HealthCheckable;

@Service
@DependsOn("eventListener")
public class HealthCheckService {
  final HealthCheckProcessor processor;

  public HealthCheckService(List<HealthCheckable> checkables) {
    checkables.forEach(
        checkable ->
            debug(String.format("[%s] is added to health check list.", checkable.getName())));
    processor = new HealthCheckProcessor(checkables);
  }

  public HealthCheckResponse check(List<String> checks) {
    return processor.check(checks);
  }
}

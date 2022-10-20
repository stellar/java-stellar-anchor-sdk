package org.stellar.anchor.platform.service;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.stellar.anchor.api.platform.HealthCheckResponse;
import org.stellar.anchor.healthcheck.HealthCheckProcessor;
import org.stellar.anchor.platform.payment.observer.stellar.StellarPaymentObserver;

@Service
public class HealthCheckService {
  HealthCheckProcessor processor;

  public HealthCheckService(Optional<StellarPaymentObserver> stellarPaymentObserver) {
    if (!stellarPaymentObserver.isEmpty()) {
      processor = new HealthCheckProcessor(List.of(stellarPaymentObserver.get()));
    } else {
      processor = new HealthCheckProcessor(List.of());
    }
  }

  public HealthCheckResponse check(List<String> checks) {
    return processor.check(checks);
  }
}

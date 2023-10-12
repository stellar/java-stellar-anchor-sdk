package org.stellar.anchor.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;

public class MetricsService {
  public Counter counter(String name, String... tags) {
    return Metrics.counter(name, tags);
  }
}

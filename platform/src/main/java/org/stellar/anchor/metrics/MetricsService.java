package org.stellar.anchor.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import org.stellar.anchor.platform.service.AnchorMetrics;

public class MetricsService {
  public Counter counter(AnchorMetrics metric, String... tags) {
    return Metrics.counter(metric.name(), tags);
  }
}

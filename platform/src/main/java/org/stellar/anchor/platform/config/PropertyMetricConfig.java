package org.stellar.anchor.platform.config;

import lombok.Data;
import org.stellar.anchor.config.MetricConfig;

@Data
public class PropertyMetricConfig implements MetricConfig {
  private boolean optionalMetricsEnabled = false;
  private Integer runInterval = 30;
}

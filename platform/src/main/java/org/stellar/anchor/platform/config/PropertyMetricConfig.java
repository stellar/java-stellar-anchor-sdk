package org.stellar.anchor.platform.config;

import lombok.Data;
import org.stellar.anchor.config.EventConfig;
import org.stellar.anchor.config.MetricConfig;

@Data
public class PropertyMetricConfig implements MetricConfig {
  private boolean enabled = false;
}

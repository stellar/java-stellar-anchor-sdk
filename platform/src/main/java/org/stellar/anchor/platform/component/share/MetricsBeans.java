package org.stellar.anchor.platform.component.share;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.stellar.anchor.platform.config.MetricConfig;

@Configuration
public class MetricsBeans {
  /**********************************
   * Metric configurations
   */
  @Bean
  @ConfigurationProperties(prefix = "metrics")
  MetricConfig metricConfig() {
    return new MetricConfig();
  }
}

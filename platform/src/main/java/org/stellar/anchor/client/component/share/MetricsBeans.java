package org.stellar.anchor.client.component.share;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.stellar.anchor.client.config.MetricConfig;
import org.stellar.anchor.metrics.MetricsService;

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

  @Bean
  MetricsService metricsService() {
    return new MetricsService();
  }
}

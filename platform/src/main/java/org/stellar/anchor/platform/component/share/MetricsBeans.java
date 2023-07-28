package org.stellar.anchor.platform.component.share;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.stellar.anchor.platform.config.MetricConfig;
import org.stellar.anchor.platform.data.JdbcSep31TransactionRepo;
import org.stellar.anchor.platform.service.MetricEmitterService;

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
  public MetricEmitterService metricService(
      MetricConfig metricConfig, JdbcSep31TransactionRepo sep31TransactionRepo) {
    return new MetricEmitterService(metricConfig, sep31TransactionRepo);
  }
}

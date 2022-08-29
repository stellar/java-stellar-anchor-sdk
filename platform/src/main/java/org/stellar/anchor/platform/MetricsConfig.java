package org.stellar.anchor.platform;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.stellar.anchor.config.MetricConfig;
import org.stellar.anchor.platform.data.JdbcSep31TransactionRepo;
import org.stellar.anchor.platform.service.MetricEmitterService;

@Configuration
public class MetricsConfig {
  @Bean
  public MetricEmitterService metricService(
      MetricConfig metricConfig, JdbcSep31TransactionRepo sep31TransactionRepo) {
    return new MetricEmitterService(metricConfig, sep31TransactionRepo);
  }
}

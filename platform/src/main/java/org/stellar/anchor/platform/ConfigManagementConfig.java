package org.stellar.anchor.platform;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.stellar.anchor.config.*;
import org.stellar.anchor.config.CirclePaymentObserverConfig;
import org.stellar.anchor.config.EventConfig;
import org.stellar.anchor.config.KafkaConfig;
import org.stellar.anchor.config.SqsConfig;
import org.stellar.anchor.payment.config.PropertyCirclePaymentConfig;
import org.stellar.anchor.paymentservice.circle.config.CirclePaymentConfig;
import org.stellar.anchor.platform.config.*;

@Configuration
public class ConfigManagementConfig {
  @Bean
  @ConfigurationProperties(prefix = "app")
  AppConfig appConfig() {
    return new PropertyAppConfig();
  }

  @Bean
  @ConfigurationProperties(prefix = "integration-auth")
  IntegrationAuthConfig integrationAuthConfig() {
    return new PropertyIntegrationAuthConfig();
  }

  @Bean
  @ConfigurationProperties(prefix = "sep1")
  Sep1Config sep1Config() {
    return new PropertySep1Config();
  }

  @Bean
  @ConfigurationProperties(prefix = "sep10")
  Sep10Config sep10Config() {
    return new PropertySep10Config();
  }

  @Bean
  @ConfigurationProperties(prefix = "sep12")
  Sep12Config sep12Config() {
    return new PropertySep12Config();
  }

  @Bean
  @ConfigurationProperties(prefix = "sep24")
  Sep24Config sep24Config() {
    return new PropertySep24Config();
  }

  @Bean
  @ConfigurationProperties(prefix = "sep31")
  Sep31Config sep31Config(CircleConfig circleConfig) {
    return new PropertySep31Config(circleConfig);
  }

  @Bean
  @ConfigurationProperties(prefix = "sep38")
  Sep38Config sep38Config() {
    return new PropertySep38Config();
  }

  @Bean
  @ConfigurationProperties(prefix = "circle")
  CircleConfig circleConfig() {
    return new PropertyCircleConfig();
  }

  @Bean
  @ConfigurationProperties(prefix = "payment-gateway.circle")
  CirclePaymentConfig circlePaymentConfig() {
    return new PropertyCirclePaymentConfig();
  }

  @Bean
  @ConfigurationProperties(prefix = "circle-payment-observer")
  CirclePaymentObserverConfig circlePaymentObserverConfig() {
    return new PropertyCirclePaymentObserverConfig();
  }

  @Bean
  @ConfigurationProperties(prefix = "event")
  EventConfig eventConfig(KafkaConfig kafkaConfig, SqsConfig sqsConfig) {
    return new PropertyEventConfig(kafkaConfig, sqsConfig);
  }

  @Bean
  @ConfigurationProperties(prefix = "kafka.publisher")
  KafkaConfig kafkaConfig() {
    return new PropertyKafkaConfig();
  }

  @Bean
  @ConfigurationProperties(prefix = "sqs.publisher")
  SqsConfig sqsConfig() {
    return new PropertySqsConfig();
  }

  @Bean
  @ConfigurationProperties(prefix = "metrics-service")
  MetricConfig metricConfig() {
    return new PropertyMetricConfig();
  }
}

package org.stellar.anchor.platform;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.stellar.anchor.auth.JwtService;
import org.stellar.anchor.config.*;
import org.stellar.anchor.platform.config.*;
import org.stellar.anchor.platform.configurator.ConfigManager;

@Configuration
public class ConfigManagementBeans {
  @Bean
  ConfigManager configManager() {
    return ConfigManager.getInstance();
  }

  @Bean
  @ConfigurationProperties()
  AppConfig appConfig() {
    return new PropertyAppConfig();
  }

  @Bean
  @ConfigurationProperties(prefix = "assets")
  AssetsConfig assetsConfig() {
    return new PropertyAssetsConfig();
  }

  @Bean
  @ConfigurationProperties(prefix = "callback-api")
  CallbackApiConfig callbackApiConfig(PropertySecretConfig secretConfig) {
    return new CallbackApiConfig(secretConfig);
  }

  @Bean
  @ConfigurationProperties(prefix = "platform-api")
  PlatformApiConfig platformApiConfig(PropertySecretConfig secretConfig) {
    return new PlatformApiConfig(secretConfig);
  }

  @Bean
  @ConfigurationProperties(prefix = "sep1")
  Sep1Config sep1Config() {
    return new PropertySep1Config();
  }

  @Bean
  @ConfigurationProperties(prefix = "sep10")
  Sep10Config sep10Config(SecretConfig secretConfig, JwtService jwtService) {
    return new PropertySep10Config(secretConfig, jwtService);
  }

  @Bean
  @ConfigurationProperties(prefix = "sep12")
  Sep12Config sep12Config(CallbackApiConfig callbackApiConfig) {
    return new PropertySep12Config(callbackApiConfig);
  }

  @Bean
  @ConfigurationProperties(prefix = "sep24")
  Sep24Config sep24Config() {
    return new PropertySep24Config();
  }

  @Bean
  @ConfigurationProperties(prefix = "sep31")
  Sep31Config sep31Config(
      PaymentObserverConfig paymentObserverConfig, CallbackApiConfig callbackApiConfig) {
    return new PropertySep31Config(callbackApiConfig, paymentObserverConfig);
  }

  @Bean
  @ConfigurationProperties(prefix = "sep38")
  Sep38Config sep38Config() {
    return new PropertySep38Config();
  }

  /**********************************
   * Payment observer configurations
   */

  @Bean
  @ConfigurationProperties(prefix = "payment-observer")
  PaymentObserverConfig paymentObserverConfig(SecretConfig secretConfig) {
    return new PropertyPaymentObserverConfig(secretConfig);
  }

  /**********************************
   * Event configurations
   */

  @Bean
  @ConfigurationProperties(prefix = "events")
  PropertyEventConfig eventConfig() {
    return new PropertyEventConfig();
  }

  @Bean
  @ConfigurationProperties(prefix = "metrics")
  MetricConfig metricConfig() {
    return new PropertyMetricConfig();
  }

  @Bean
  @ConfigurationProperties
  PropertySecretConfig secretConfig() {
    return new PropertySecretConfig();
  }
}

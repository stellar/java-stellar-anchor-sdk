package org.stellar.anchor.platform;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.stellar.anchor.config.*;
import org.stellar.anchor.platform.config.*;
import org.stellar.anchor.platform.configurator.ConfigManager;
import org.stellar.anchor.platform.configurator.ObserverConfigManager;
import org.stellar.anchor.platform.configurator.SepConfigManager;

@Configuration
public class ConfigManagementBeans {
  @Bean(name = "configManager")
  @Profile("stellar-observer")
  ConfigManager observerConfigManager() {
    return ObserverConfigManager.getInstance();
  }

  @Bean(name = "configManager")
  @Profile("default")
  ConfigManager configManager() {
    return SepConfigManager.getInstance();
  }

  @Bean
  @ConfigurationProperties(prefix = "")
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

  /**********************************
   * SEP configurations
   */
  @Bean
  @ConfigurationProperties(prefix = "sep1")
  Sep1Config sep1Config() {
    return new PropertySep1Config();
  }

  @Bean
  @ConfigurationProperties(prefix = "sep10")
  Sep10Config sep10Config(SecretConfig secretConfig) {
    return new PropertySep10Config(secretConfig);
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
  Sep31Config sep31Config() {
    return new PropertySep31Config();
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
  public PaymentObserverConfig paymentObserverConfig() {
    return new PaymentObserverConfig();
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

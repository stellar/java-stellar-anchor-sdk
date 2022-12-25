package org.stellar.anchor.platform.component.observer;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.stellar.anchor.platform.config.PaymentObserverConfig;
import org.stellar.anchor.platform.configurator.ConfigManager;
import org.stellar.anchor.platform.configurator.ObserverConfigManager;

@Configuration
public class ConfigBeans {
  @Bean(name = "configManager")
  ConfigManager observerConfigManager() {
    return ObserverConfigManager.getInstance();
  }

  /**********************************
   * Payment observer configurations
   */

  @Bean
  @ConfigurationProperties(prefix = "payment-observer")
  public PaymentObserverConfig paymentObserverConfig() {
    return new PaymentObserverConfig();
  }
}

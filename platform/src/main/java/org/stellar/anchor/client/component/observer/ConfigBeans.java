package org.stellar.anchor.client.component.observer;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.stellar.anchor.client.config.PaymentObserverConfig;
import org.stellar.anchor.client.configurator.ConfigManager;
import org.stellar.anchor.client.configurator.ObserverConfigManager;

@Configuration
public class ConfigBeans {
  @Bean(name = "configManager")
  ConfigManager observerConfigManager() {
    return ObserverConfigManager.getInstance();
  }

  @Bean
  @ConfigurationProperties(prefix = "payment-observer")
  public PaymentObserverConfig paymentObserverConfig() {
    return new PaymentObserverConfig();
  }
}

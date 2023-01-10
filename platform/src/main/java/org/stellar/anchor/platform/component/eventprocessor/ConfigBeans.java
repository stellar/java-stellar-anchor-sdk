package org.stellar.anchor.platform.component.eventprocessor;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.stellar.anchor.platform.config.EventProcessorConfig;
import org.stellar.anchor.platform.configurator.ConfigManager;
import org.stellar.anchor.platform.configurator.ObserverConfigManager;

@Configuration
public class ConfigBeans {
  @Bean(name = "configManager")
  ConfigManager observerConfigManager() {
    return ObserverConfigManager.getInstance();
  }

  @Bean
  @ConfigurationProperties(prefix = "event-processor")
  public EventProcessorConfig eventProcessorConfig() {
    return new EventProcessorConfig();
  }
}

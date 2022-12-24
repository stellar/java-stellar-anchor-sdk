package org.stellar.anchor.platform.component.observer;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.stellar.anchor.platform.configurator.ConfigManager;
import org.stellar.anchor.platform.configurator.ObserverConfigManager;

@Configuration
public class ConfigManagerBean {
  @Bean(name = "configManager")
  ConfigManager observerConfigManager() {
    return ObserverConfigManager.getInstance();
  }
}

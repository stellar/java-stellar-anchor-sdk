package org.stellar.anchor.client.component.platform;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.stellar.anchor.client.configurator.ConfigManager;
import org.stellar.anchor.client.configurator.PlatformConfigManager;

@Configuration
public class ConfigBeans {
  @Bean(name = "configManager")
  ConfigManager configManager() {
    return PlatformConfigManager.getInstance();
  }
}

package org.stellar.anchor.platform.component.platform;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.stellar.anchor.platform.configurator.ConfigManager;
import org.stellar.anchor.platform.configurator.PlatformConfigManager;

@Configuration
public class ConfigBeans {
  @Bean(name = "configManager")
  ConfigManager configManager() {
    return PlatformConfigManager.getInstance();
  }
}

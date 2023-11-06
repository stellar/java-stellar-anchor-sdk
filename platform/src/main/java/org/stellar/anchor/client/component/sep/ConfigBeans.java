package org.stellar.anchor.client.component.sep;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.stellar.anchor.client.configurator.ConfigManager;
import org.stellar.anchor.client.configurator.SepConfigManager;

@Configuration
public class ConfigBeans {
  @Bean(name = "configManager")
  ConfigManager configManager() {
    return SepConfigManager.getInstance();
  }
}

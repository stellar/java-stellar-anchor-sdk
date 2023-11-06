package org.stellar.anchor.client.component.custody;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.stellar.anchor.client.configurator.ConfigManager;
import org.stellar.anchor.client.configurator.CustodyConfigManager;

@Configuration
public class ConfigBeans {

  @Bean(name = "configManager")
  ConfigManager configManager() {
    return CustodyConfigManager.getInstance();
  }
}

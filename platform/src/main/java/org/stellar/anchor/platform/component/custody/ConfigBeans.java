package org.stellar.anchor.platform.component.custody;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.stellar.anchor.platform.config.CustodySecretConfig;
import org.stellar.anchor.platform.config.PropertyCustodySecretConfig;
import org.stellar.anchor.platform.configurator.ConfigManager;
import org.stellar.anchor.platform.configurator.CustodyConfigManager;

@Configuration
public class ConfigBeans {
  @Bean(name = "configManager")
  ConfigManager configManager() {
    return CustodyConfigManager.getInstance();
  }

  @Bean
  CustodySecretConfig custodySecretConfig() {
    return new PropertyCustodySecretConfig();
  }
}

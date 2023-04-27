package org.stellar.anchor.platform.component.custody;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.stellar.anchor.config.SecretConfig;
import org.stellar.anchor.platform.config.FireblocksConfig;
import org.stellar.anchor.platform.configurator.ConfigManager;
import org.stellar.anchor.platform.configurator.CustodyConfigManager;

@Configuration
public class ConfigBeans {
  @Bean(name = "configManager")
  ConfigManager configManager() {
    return CustodyConfigManager.getInstance();
  }

  @Bean
  @ConfigurationProperties(prefix = "custody.fireblocks")
  FireblocksConfig fireblocksConfig(
      SecretConfig secretConfig, @Value("custody.type") String custodyType) {
    boolean fireblocksIntegrationEnabled = "fireblocks".equals(custodyType);
    return new FireblocksConfig(secretConfig, fireblocksIntegrationEnabled);
  }
}

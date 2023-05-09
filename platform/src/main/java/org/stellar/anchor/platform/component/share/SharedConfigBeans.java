package org.stellar.anchor.platform.component.share;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.stellar.anchor.platform.config.CustodyApiConfig;
import org.stellar.anchor.platform.config.CustodySecretConfig;
import org.stellar.anchor.platform.config.PlatformApiConfig;
import org.stellar.anchor.platform.config.PropertyCustodySecretConfig;
import org.stellar.anchor.platform.config.PropertySecretConfig;

@Configuration
public class SharedConfigBeans {
  @Bean
  @ConfigurationProperties(prefix = "platform-api")
  PlatformApiConfig platformApiConfig(PropertySecretConfig secretConfig) {
    return new PlatformApiConfig(secretConfig);
  }

  @Bean
  CustodySecretConfig custodySecretConfig() {
    return new PropertyCustodySecretConfig();
  }

  @Bean
  @ConfigurationProperties(prefix = "custody-server")
  public CustodyApiConfig custodyApiConfig(CustodySecretConfig custodySecretConfig) {
    return new CustodyApiConfig(custodySecretConfig);
  }
}

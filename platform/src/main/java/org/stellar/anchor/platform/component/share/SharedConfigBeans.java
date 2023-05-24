package org.stellar.anchor.platform.component.share;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.stellar.anchor.config.SecretConfig;
import org.stellar.anchor.platform.config.PlatformApiConfig;
import org.stellar.anchor.platform.config.PropertyDataConfig;
import org.stellar.anchor.platform.config.PropertySecretConfig;

@Configuration
public class SharedConfigBeans {
  @Bean
  @ConfigurationProperties(prefix = "data")
  PropertyDataConfig dataConfig(SecretConfig secretConfig) {
    return new PropertyDataConfig(secretConfig);
  }

  @Bean
  @ConfigurationProperties(prefix = "platform-api")
  PlatformApiConfig platformApiConfig(PropertySecretConfig secretConfig) {
    return new PlatformApiConfig(secretConfig);
  }
}

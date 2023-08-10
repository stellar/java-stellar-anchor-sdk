package org.stellar.anchor.platform.component.share;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.stellar.anchor.platform.config.ClientsConfig;

@Configuration
public class ClientsBeans {
  @Bean
  @ConfigurationProperties(prefix = "")
  ClientsConfig clientsConfig() {
    return new ClientsConfig();
  }
}

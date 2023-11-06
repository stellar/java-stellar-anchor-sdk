package org.stellar.anchor.client.component.share;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.stellar.anchor.client.config.PropertyClientsConfig;

@Configuration
public class ClientsBeans {
  @Bean
  @ConfigurationProperties(prefix = "")
  PropertyClientsConfig clientsConfig() {
    return new PropertyClientsConfig();
  }
}

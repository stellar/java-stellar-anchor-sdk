package org.stellar.anchor.platform.component.share;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.stellar.anchor.client.ClientFinder;
import org.stellar.anchor.config.Sep10Config;
import org.stellar.anchor.platform.config.PropertyClientsConfig;

@Configuration
public class ClientsBeans {
  @Bean
  @ConfigurationProperties(prefix = "")
  PropertyClientsConfig clientsConfig() {
    return new PropertyClientsConfig();
  }

  @Bean
  @ConfigurationProperties(prefix = "")
  ClientFinder clientFinder(Sep10Config sep10Config, PropertyClientsConfig clientsConfig) {
    return new ClientFinder(sep10Config, clientsConfig);
  }
}

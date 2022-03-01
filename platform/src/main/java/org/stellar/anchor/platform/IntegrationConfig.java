package org.stellar.anchor.platform;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.stellar.anchor.config.Sep12Config;
import org.stellar.anchor.integration.customer.CustomerIntegration;

@Configuration
public class IntegrationConfig {
  @Bean
  CustomerIntegration customerIntegration(Sep12Config sep12Config) {
    return new NettyCustomerIntegration(sep12Config.getCustomerIntegrationEndPoint());
  }
}

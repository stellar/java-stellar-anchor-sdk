package org.stellar.anchor.client.component.share;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.stellar.anchor.client.config.RpcConfig;

@Configuration
public class RpcBeans {
  @Bean
  @ConfigurationProperties(prefix = "rpc")
  RpcConfig rpcConfig() {
    return new RpcConfig();
  }
}

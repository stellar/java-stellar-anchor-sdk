package org.stellar.anchor.client.component.share;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.stellar.anchor.client.config.AppLoggingConfig;
import org.stellar.anchor.client.config.CallbackApiConfig;
import org.stellar.anchor.client.config.CustodyApiConfig;
import org.stellar.anchor.client.config.PlatformApiConfig;
import org.stellar.anchor.client.config.PlatformServerConfig;
import org.stellar.anchor.client.config.PropertyCustodyConfig;
import org.stellar.anchor.client.config.PropertyCustodySecretConfig;
import org.stellar.anchor.client.config.PropertyDataConfig;
import org.stellar.anchor.client.config.PropertySecretConfig;
import org.stellar.anchor.config.CustodySecretConfig;
import org.stellar.anchor.config.SecretConfig;

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

  @Bean
  CustodySecretConfig custodySecretConfig() {
    return new PropertyCustodySecretConfig();
  }

  @Bean
  @ConfigurationProperties(prefix = "custody-server")
  public CustodyApiConfig custodyApiConfig(CustodySecretConfig custodySecretConfig) {
    return new CustodyApiConfig(custodySecretConfig);
  }

  @Bean
  @ConfigurationProperties(prefix = "custody")
  PropertyCustodyConfig propertyCustodyConfig() {
    return new PropertyCustodyConfig();
  }

  @Bean
  @ConfigurationProperties(prefix = "callback-api")
  CallbackApiConfig callbackApiConfig(PropertySecretConfig secretConfig) {
    return new CallbackApiConfig(secretConfig);
  }

  @Bean
  @ConfigurationProperties(prefix = "platform-server")
  PlatformServerConfig platformServerConfig(PropertySecretConfig secretConfig) {
    return new PlatformServerConfig(secretConfig);
  }

  @Bean
  @ConfigurationProperties(prefix = "app-logging")
  AppLoggingConfig appLoggingConfig() {
    return new AppLoggingConfig();
  }
}

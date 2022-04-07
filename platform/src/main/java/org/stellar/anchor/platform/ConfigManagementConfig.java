package org.stellar.anchor.platform;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.stellar.anchor.config.*;
import org.stellar.anchor.config.AppConfig;
import org.stellar.anchor.config.Sep10Config;
import org.stellar.anchor.config.Sep1Config;
import org.stellar.anchor.config.Sep38Config;
import org.stellar.anchor.server.config.*;
import org.stellar.anchor.server.config.PropertyAppConfig;
import org.stellar.anchor.server.config.PropertySep10Config;
import org.stellar.anchor.server.config.PropertySep1Config;
import org.stellar.anchor.server.config.PropertySep38Config;

@Configuration
public class ConfigManagementConfig {
  @Bean
  @ConfigurationProperties(prefix = "app")
  AppConfig appConfig() {
    return new PropertyAppConfig();
  }

  @Bean
  @ConfigurationProperties(prefix = "sep1")
  Sep1Config sep1Config() {
    return new PropertySep1Config();
  }

  @Bean
  @ConfigurationProperties(prefix = "sep10")
  Sep10Config sep10Config() {
    return new PropertySep10Config();
  }

  @Bean
  @ConfigurationProperties(prefix = "sep12")
  Sep12Config sep12Config() {
    return new PropertySep12Config();
  }

  @Bean
  @ConfigurationProperties(prefix = "sep31")
  Sep31Config sep31Config() {
    return new PropertySep31Config();
  }

  @Bean
  @ConfigurationProperties(prefix = "sep38")
  Sep38Config sep38Config() {
    return new PropertySep38Config();
  }

  @Bean
  @ConfigurationProperties(prefix = "event")
  EventConfig eventConfig() {
    return new PropertyEventConfig();
  }
}

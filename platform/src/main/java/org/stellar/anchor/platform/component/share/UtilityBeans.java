package org.stellar.anchor.platform.component.share;

import com.google.gson.Gson;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.stellar.anchor.config.AppConfig;
import org.stellar.anchor.healthcheck.HealthCheckable;
import org.stellar.anchor.platform.config.PropertyAppConfig;
import org.stellar.anchor.platform.config.PropertySecretConfig;
import org.stellar.anchor.platform.service.HealthCheckService;
import org.stellar.anchor.util.GsonUtils;

@Configuration
public class UtilityBeans {
  @Bean
  public Gson gson() {
    return GsonUtils.builder().create();
  }

  @Bean
  @DependsOn("configManager")
  public HealthCheckService healthCheckService(List<HealthCheckable> checkables) {
    return new HealthCheckService(checkables);
  }

  @Bean
  @ConfigurationProperties(prefix = "")
  AppConfig appConfig() {
    return new PropertyAppConfig();
  }

  /**********************************
   * Secret configurations
   */
  @Bean
  @ConfigurationProperties
  PropertySecretConfig secretConfig() {
    return new PropertySecretConfig();
  }
}

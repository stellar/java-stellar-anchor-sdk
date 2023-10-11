package org.stellar.anchor.platform.component.share;

import com.google.gson.Gson;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.stellar.anchor.api.exception.NotSupportedException;
import org.stellar.anchor.auth.JwtService;
import org.stellar.anchor.config.AppConfig;
import org.stellar.anchor.config.SecretConfig;
import org.stellar.anchor.healthcheck.HealthCheckable;
import org.stellar.anchor.horizon.Horizon;
import org.stellar.anchor.platform.config.ClientsConfig;
import org.stellar.anchor.platform.config.PropertyAppConfig;
import org.stellar.anchor.platform.config.PropertySecretConfig;
import org.stellar.anchor.platform.config.PropertySep24Config;
import org.stellar.anchor.platform.service.HealthCheckService;
import org.stellar.anchor.platform.service.SimpleMoreInfoUrlConstructor;
import org.stellar.anchor.sep24.MoreInfoUrlConstructor;
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

  @Bean
  MoreInfoUrlConstructor moreInfoUrlConstructor(
      ClientsConfig clientsConfig, PropertySep24Config sep24Config, JwtService jwtService) {
    return new SimpleMoreInfoUrlConstructor(
        clientsConfig, sep24Config.getMoreInfoUrl(), jwtService);
  }

  @Bean
  @ConfigurationProperties(prefix = "sep24")
  PropertySep24Config sep24Config(SecretConfig secretConfig) {
    return new PropertySep24Config(secretConfig);
  }

  /**********************************
   * Secret configurations
   */
  @Bean
  PropertySecretConfig secretConfig() {
    return new PropertySecretConfig();
  }

  @Bean
  public JwtService jwtService(SecretConfig secretConfig) throws NotSupportedException {
    return new JwtService(secretConfig);
  }

  @Bean
  public Horizon horizon(AppConfig appConfig) {
    return new Horizon(appConfig);
  }
}

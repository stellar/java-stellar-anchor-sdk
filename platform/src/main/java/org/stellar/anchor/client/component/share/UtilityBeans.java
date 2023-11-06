package org.stellar.anchor.client.component.share;

import com.google.gson.Gson;
import java.util.List;
import javax.validation.Validator;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.stellar.anchor.api.exception.NotSupportedException;
import org.stellar.anchor.auth.JwtService;
import org.stellar.anchor.client.config.*;
import org.stellar.anchor.client.service.HealthCheckService;
import org.stellar.anchor.client.service.SimpleMoreInfoUrlConstructor;
import org.stellar.anchor.client.validator.RequestValidator;
import org.stellar.anchor.config.*;
import org.stellar.anchor.healthcheck.HealthCheckable;
import org.stellar.anchor.horizon.Horizon;
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
      PropertyClientsConfig clientsConfig, PropertySep24Config sep24Config, JwtService jwtService) {
    return new SimpleMoreInfoUrlConstructor(
        clientsConfig, sep24Config.getMoreInfoUrl(), jwtService);
  }

  @Bean
  @ConfigurationProperties(prefix = "sep24")
  PropertySep24Config sep24Config(SecretConfig secretConfig, CustodyConfig custodyConfig) {
    return new PropertySep24Config(secretConfig, custodyConfig);
  }

  @Bean
  @ConfigurationProperties(prefix = "sep6")
  PropertySep6Config sep6Config(CustodyConfig custodyConfig) {
    return new PropertySep6Config(custodyConfig);
  }

  /**********************************
   * Secret configurations
   */
  @Bean
  PropertySecretConfig secretConfig() {
    return new PropertySecretConfig();
  }

  @Bean
  public JwtService jwtService(SecretConfig secretConfig, CustodySecretConfig custodySecretConfig)
      throws NotSupportedException {
    return new JwtService(secretConfig, custodySecretConfig);
  }

  @Bean
  public Horizon horizon(AppConfig appConfig) {
    return new Horizon(appConfig);
  }

  @Bean
  public RequestValidator requestValidator(Validator validator) {
    return new RequestValidator(validator);
  }
}

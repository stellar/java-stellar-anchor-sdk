package org.stellar.anchor.platform.config;

import lombok.Data;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.stellar.anchor.config.CircleConfig;
import org.stellar.anchor.config.Sep12Config;

@Data
public class PropertySep12Config implements Sep12Config, Validator {
  Boolean enabled = false;
  String customerIntegrationEndPoint;

  public PropertySep12Config(CallbackApiConfig callbackApiConfig) {
    this.customerIntegrationEndPoint = callbackApiConfig.getBaseUrl();
  }

  @Override
  public boolean supports(Class<?> clazz) {
    return Sep12Config.class.isAssignableFrom(clazz);
  }

  @Override
  public void validate(Object target, Errors errors) {
    Sep12Config config = (Sep12Config) target;
  }
}

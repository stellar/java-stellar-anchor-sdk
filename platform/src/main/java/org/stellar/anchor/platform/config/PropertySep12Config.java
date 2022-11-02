package org.stellar.anchor.platform.config;

import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.stellar.anchor.config.Sep12Config;

@Data
public class PropertySep12Config implements Sep12Config, Validator {
  Boolean enabled;
  String customerIntegrationEndPoint;

  public PropertySep12Config(CallbackApiConfig callbackApiConfig) {
    this.customerIntegrationEndPoint = callbackApiConfig.getBaseUrl();
  }

  @Override
  public boolean supports(@NotNull Class<?> clazz) {
    return Sep12Config.class.isAssignableFrom(clazz);
  }

  @Override
  public void validate(@NotNull Object target, @NotNull Errors errors) {
    Sep12Config config = (Sep12Config) target;
  }
}

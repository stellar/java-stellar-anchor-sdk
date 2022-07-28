package org.stellar.anchor.platform.config;

import lombok.Data;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;
import org.stellar.anchor.config.Sep12Config;
import org.stellar.anchor.util.UrlValidationUtil;

@Data
public class PropertySep12Config implements Sep12Config, Validator {
  Boolean enabled = false;
  String customerIntegrationEndPoint;

  @Override
  public boolean supports(Class<?> clazz) {
    return Sep12Config.class.isAssignableFrom(clazz);
  }

  @Override
  public void validate(Object target, Errors errors) {
    Sep12Config config = (Sep12Config) target;

    if (config.getEnabled()) {
      ValidationUtils.rejectIfEmpty(
          errors, "customerIntegrationEndPoint", "empty-customerIntegrationEndPoint");

      UrlValidationUtil.rejectIfMalformed(
          config.getCustomerIntegrationEndPoint(), "customerIntegrationEndPoint", errors);
    }
  }
}

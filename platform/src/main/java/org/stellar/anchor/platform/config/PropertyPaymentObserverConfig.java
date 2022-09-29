package org.stellar.anchor.platform.config;

import static org.stellar.anchor.util.StringHelper.isEmpty;

import lombok.Data;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;
import org.stellar.anchor.auth.JwtService;
import org.stellar.anchor.config.PaymentObserverConfig;
import org.stellar.anchor.config.SecretConfig;

@Data
public class PropertyPaymentObserverConfig implements PaymentObserverConfig, Validator {
  private boolean enabled = false;
  private String trackedWallet = "all";

  private SecretConfig secretConfig;

  public PropertyPaymentObserverConfig(SecretConfig secretConfig) {
    this.secretConfig = secretConfig;
  }

  public String getApiKey() {
    return this.secretConfig.getCircleApiKey();
  }

  @Override
  public boolean supports(Class<?> clazz) {
    return PropertyPaymentObserverConfig.class.isAssignableFrom(clazz);
  }

  @Override
  public void validate(Object target, Errors errors) {
    PropertyPaymentObserverConfig config = (PropertyPaymentObserverConfig) target;

    if (config.enabled) {
      ValidationUtils.rejectIfEmptyOrWhitespace(errors, "trackedWallet", "empty-trackedWallet");

      if (isEmpty(secretConfig.getCircleApiKey())) {
        errors.rejectValue(
            null,
            "empty-circleApiKey",
            "Please set environment variable secret.circle.api_key");
      }
    }
  }
}

package org.stellar.anchor.platform.config;

import lombok.Data;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;
import org.stellar.anchor.config.PaymentObserverConfig;

@Data
public class PropertyPaymentObserverConfig implements PaymentObserverConfig, Validator {
  private boolean enabled = false;
  private String trackedWallet = "all";
  private String circleUrl;

  @Override
  public boolean supports(Class<?> clazz) {
    return PaymentObserverConfig.class.isAssignableFrom(clazz);
  }

  @Override
  public void validate(Object target, Errors errors) {
    PropertyPaymentObserverConfig config = (PropertyPaymentObserverConfig) target;

    if (config.enabled) {
      ValidationUtils.rejectIfEmptyOrWhitespace(errors, "trackedWallet", "empty-trackedWallet");
    }
  }
}

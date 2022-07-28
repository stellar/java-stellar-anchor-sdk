package org.stellar.anchor.platform.config;

import lombok.Data;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;
import org.stellar.anchor.config.CirclePaymentObserverConfig;

@Data
public class PropertyCirclePaymentObserverConfig implements CirclePaymentObserverConfig, Validator {
  private boolean enabled = false;
  private String trackedWallet = "all";

  @Override
  public boolean supports(Class<?> clazz) {
    return CirclePaymentObserverConfig.class.isAssignableFrom(clazz);
  }

  @Override
  public void validate(Object target, Errors errors) {
    PropertyCirclePaymentObserverConfig config = (PropertyCirclePaymentObserverConfig) target;

    if (config.enabled) {
      ValidationUtils.rejectIfEmptyOrWhitespace(errors, "trackedWallet", "empty-trackedWallet");
    }
  }
}

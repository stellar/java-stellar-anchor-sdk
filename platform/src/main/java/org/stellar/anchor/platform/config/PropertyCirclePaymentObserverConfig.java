package org.stellar.anchor.platform.config;

import lombok.Data;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;
import org.stellar.anchor.config.CirclePaymentObserverConfig;

@Data
public class PropertyCirclePaymentObserverConfig implements CirclePaymentObserverConfig, Validator {
  private boolean enabled = false;
  private String stellarNetwork = "TESTNET";
  private String horizonUrl = "https://horizon-testnet.stellar.org";
  private String trackedWallet = "all";

  @Override
  public boolean supports(Class<?> clazz) {
    return CirclePaymentObserverConfig.class.isAssignableFrom(clazz);
  }

  @Override
  public void validate(Object target, Errors errors) {
    CirclePaymentObserverConfig config = (CirclePaymentObserverConfig) target;

    ValidationUtils.rejectIfEmpty(errors, "stellarNetwork", "empty-stellarNetwork");
    ValidationUtils.rejectIfEmpty(errors, "horizonUrl", "empty-horizonUrl");
    ValidationUtils.rejectIfEmpty(errors, "trackedWallet", "empty-trackedWallet");
  }
}
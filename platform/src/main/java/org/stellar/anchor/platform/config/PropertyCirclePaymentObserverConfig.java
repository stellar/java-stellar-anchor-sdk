package org.stellar.anchor.platform.config;

import lombok.Data;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;
import org.stellar.anchor.config.AppConfig;
import org.stellar.anchor.config.CirclePaymentObserverConfig;
import org.stellar.anchor.util.Log;
import org.stellar.anchor.util.UrlConnectionStatus;
import org.stellar.anchor.util.UrlValidationUtil;

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
    PropertyCirclePaymentObserverConfig config = (PropertyCirclePaymentObserverConfig) target;

    ValidationUtils.rejectIfEmptyOrWhitespace(errors, "stellarNetwork", "empty-stellarNetwork");
    ValidationUtils.rejectIfEmptyOrWhitespace(errors, "horizonUrl", "empty-horizonUrl");
    ValidationUtils.rejectIfEmptyOrWhitespace(errors, "trackedWallet", "empty-trackedWallet");

    UrlConnectionStatus horizonUrlStatus = UrlValidationUtil.validateUrl(config.getHorizonUrl());
    if (horizonUrlStatus == UrlConnectionStatus.MALFORMED) {
      errors.rejectValue("horizonUrl", "invalidUrl-horizonUrl", "horizon url is not in valid format");
    } else if (horizonUrlStatus == UrlConnectionStatus.UNREACHABLE) {
      Log.error("horizonUrl field invalid: cannot connect to horizon url");
    }
  }
}
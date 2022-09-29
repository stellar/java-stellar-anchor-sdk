package org.stellar.anchor.platform.config;

import static org.stellar.anchor.config.Sep31Config.DepositInfoGeneratorType.*;
import static org.stellar.anchor.config.Sep31Config.PaymentType.STRICT_SEND;

import java.util.Objects;
import lombok.Data;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.stellar.anchor.config.PaymentObserverConfig;
import org.stellar.anchor.config.Sep31Config;

@Data
public class PropertySep31Config implements Sep31Config, Validator {
  boolean enabled;
  String feeIntegrationEndPoint;
  String uniqueAddressIntegrationEndPoint;
  PaymentType paymentType = STRICT_SEND;
  DepositInfoGeneratorType depositInfoGeneratorType = SELF;

  PaymentObserverConfig paymentObserverConfig;

  public PropertySep31Config(CallbackApiConfig callbackApiConfig, PaymentObserverConfig paymentObserverConfig) {
    this.paymentObserverConfig = paymentObserverConfig;
    this.feeIntegrationEndPoint = callbackApiConfig.getBaseUrl();
    this.uniqueAddressIntegrationEndPoint = callbackApiConfig.getBaseUrl();
  }

  @Override
  public boolean supports(Class<?> clazz) {
    return Sep31Config.class.isAssignableFrom(clazz);
  }

  @Override
  public void validate(Object target, Errors errors) {
    Sep31Config config = (Sep31Config) target;
    if (config.isEnabled()) {
      /*if (config.getDepositInfoGeneratorType().equals(CIRCLE)) {
        if (paymentObserverConfig.validate().hasErrors()) {
          errors.rejectValue(
              "paymentObserverConfig",
              "badConfig-paymentObserverConfig",
              "depositInfoGeneratorType set as circle, but circle config not properly configured");
        }
      } else */
      if (config.getDepositInfoGeneratorType().equals(API)) {
        if (Objects.toString(uniqueAddressIntegrationEndPoint, "").isEmpty()) {
          errors.rejectValue(
              "uniqueAddressIntegrationEndPoint",
              "badConfig-uniqueAddressIntegrationEndPoint",
              "depositInfoGeneratorType set as API, but uniqueAddressIntegrationEndPoint not properly configured");
        }
      }
    }
  }
}

package org.stellar.anchor.platform.config;

import static org.stellar.anchor.config.Sep31Config.DepositInfoGeneratorType.CUSTODY;
import static org.stellar.anchor.config.Sep31Config.PaymentType.STRICT_SEND;

import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.stellar.anchor.config.CustodyConfig;
import org.stellar.anchor.config.Sep31Config;

@Data
public class PropertySep31Config implements Sep31Config, Validator {
  boolean enabled;
  PaymentType paymentType = STRICT_SEND;
  DepositInfoGeneratorType depositInfoGeneratorType;
  CustodyConfig custodyConfig;

  public PropertySep31Config(CustodyConfig custodyConfig) {
    this.custodyConfig = custodyConfig;
  }

  @Override
  public boolean supports(@NotNull Class<?> clazz) {
    return Sep31Config.class.isAssignableFrom(clazz);
  }

  @Override
  public void validate(@NotNull Object target, @NotNull Errors errors) {
    if (enabled) {
      validateDepositInfoGeneratorType(errors);
    }
  }

  void validateDepositInfoGeneratorType(Errors errors) {
    if (custodyConfig.isCustodyIntegrationEnabled() && CUSTODY != depositInfoGeneratorType) {
      errors.rejectValue(
          "depositInfoGeneratorType",
          "sep31-deposit-info-generator-type",
          String.format(
              "[%s] deposit info generator type is not supported when custody integration is enabled",
              depositInfoGeneratorType.toString().toLowerCase()));
    } else if (!custodyConfig.isCustodyIntegrationEnabled()
        && CUSTODY == depositInfoGeneratorType) {
      errors.rejectValue(
          "depositInfoGeneratorType",
          "sep31-deposit-info-generator-type",
          "[custody] deposit info generator type is not supported when custody integration is disabled");
    }
  }
}

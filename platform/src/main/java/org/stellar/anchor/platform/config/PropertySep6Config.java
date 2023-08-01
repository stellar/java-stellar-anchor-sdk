package org.stellar.anchor.platform.config;

import lombok.*;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.stellar.anchor.config.Sep6Config;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PropertySep6Config implements Sep6Config, Validator {
  boolean enabled;
  Features features;

  @Override
  public boolean supports(@NonNull Class<?> clazz) {
    return Sep6Config.class.isAssignableFrom(clazz);
  }

  @Override
  public void validate(@NonNull Object target, @NonNull Errors errors) {
    if (enabled) {
      if (features == null) {
        errors.rejectValue("features", "sep6-features-invalid", "sep6.features is not defined");
      } else if (features.isAccountCreation()) {
        errors.rejectValue(
            "features",
            "sep6-features-account-creation-invalid",
            "sep6.features.account_creation: account creation is not supported");
      } else if (features.isClaimableBalances()) {
        errors.rejectValue(
            "features",
            "sep6-features-claimable-balances-invalid",
            "sep6.features.claimable_balances: claimable balances are not supported");
      }
    }
  }
}

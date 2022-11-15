package org.stellar.anchor.platform.config;

import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.stellar.anchor.config.Sep24Config;

@Data
public class PropertySep24Config implements Sep24Config, Validator {
  boolean enabled;
  int interactiveJwtExpiration;
  String interactiveUrl;

  @Override
  public boolean supports(@NotNull Class<?> clazz) {
    return Sep24Config.class.isAssignableFrom(clazz);
  }

  @Override
  public void validate(@NotNull Object target, @NotNull Errors errors) {
    Sep24Config config = (Sep24Config) target;
  }
}

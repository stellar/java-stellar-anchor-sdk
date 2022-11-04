package org.stellar.anchor.platform.config;

import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.stellar.anchor.config.Sep38Config;

@Data
public class PropertySep38Config implements Sep38Config, Validator {
  boolean enabled;

  @Override
  public boolean supports(@NotNull Class<?> clazz) {
    return Sep38Config.class.isAssignableFrom(clazz);
  }

  @Override
  public void validate(@NotNull Object target, @NotNull Errors errors) {
    Sep38Config config = (Sep38Config) target;
  }
}

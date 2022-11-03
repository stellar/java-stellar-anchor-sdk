package org.stellar.anchor.platform.config;

import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.stellar.anchor.config.AppConfig;
import org.stellar.anchor.config.MetricConfig;

@Data
public class PropertyMetricConfig implements MetricConfig, Validator {
  private boolean enbaled = false;
  private boolean extrasEnabled = false;
  private Integer runInterval = 30;

  @Override
  public boolean supports(@NotNull Class<?> clazz) {
    return AppConfig.class.isAssignableFrom(clazz);
  }

  @Override
  public void validate(@NotNull Object target, @NotNull Errors errors) {
    System.out.println("here");
  }

  @Override
  public boolean isExtrasEnabled() {
    return this.extrasEnabled;
  }
}

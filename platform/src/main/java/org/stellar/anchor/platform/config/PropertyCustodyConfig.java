package org.stellar.anchor.platform.config;

import static org.stellar.anchor.util.StringHelper.isEmpty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.stellar.anchor.config.CustodyConfig;

@Data
public class PropertyCustodyConfig implements CustodyConfig, Validator {

  private String type;
  private HttpClientConfig httpClient;
  private TrustLine trustLine;

  @Override
  public boolean supports(@NotNull Class<?> clazz) {
    return PropertyCustodyConfig.class.isAssignableFrom(clazz);
  }

  @Override
  public void validate(@NotNull Object target, @NotNull Errors errors) {
    validateType(errors);
    if (isCustodyIntegrationEnabled()) {
      httpClient.validate("custody", errors);
      validateCheckCronExpression(errors);
      validateCheckDuration(errors);
    }
  }

  private void validateType(Errors errors) {
    if (isEmpty(type)) {
      errors.rejectValue(
          "type", "custody-type-empty", "The custody.type cannot be empty and must be defined");
    }
  }

  private void validateCheckCronExpression(Errors errors) {
    if (isEmpty(trustLine.checkCronExpression)) {
      errors.reject(
          "custody-trust_line-check_cron_expression-empty",
          "The custody.trust_line.check_cron_expression is empty");
    }
    if (!CronExpression.isValidExpression(trustLine.checkCronExpression)) {
      errors.reject(
          "custody-trust_line-check_cron_expression-invalid",
          "The custody.trust_line.check_cron_expression is invalid");
    }
  }

  private void validateCheckDuration(Errors errors) {
    if (trustLine.checkDuration <= 0) {
      errors.reject(
          "custody-trust_line-check_duration-invalid",
          "trust_line-check_duration must be greater than 0");
    }
  }

  @Data
  @AllArgsConstructor
  @NoArgsConstructor
  public static class TrustLine {
    private String checkCronExpression;
    private int checkDuration;
    private String timeoutMessage;
  }
}

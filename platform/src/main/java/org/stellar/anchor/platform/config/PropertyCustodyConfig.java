package org.stellar.anchor.platform.config;

import static org.stellar.anchor.util.StringHelper.isEmpty;

import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.stellar.anchor.config.CustodyConfig;

@Data
public class PropertyCustodyConfig implements CustodyConfig, Validator {

  private String type;
  private HttpClientConfig httpClient;

  @Override
  public boolean supports(@NotNull Class<?> clazz) {
    return PropertyCustodyConfig.class.isAssignableFrom(clazz);
  }

  @Override
  public void validate(@NotNull Object target, @NotNull Errors errors) {
    validateType(errors);
    if (this.isCustodyIntegrationEnabled()) {
      httpClient.validate("custody", errors);
    }
  }

  private void validateType(Errors errors) {
    if (isEmpty(type)) {
      errors.rejectValue(
          "type", "custody-type-empty", "The custody.type cannot be empty and must be defined");
    }
  }
}

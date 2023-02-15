package org.stellar.anchor.platform.config;

import static org.stellar.anchor.platform.config.PropertySecretConfig.SECRET_DATA_PASSWORD;
import static org.stellar.anchor.platform.config.PropertySecretConfig.SECRET_DATA_USERNAME;
import static org.stellar.anchor.platform.configurator.DataConfigAdapter.*;
import static org.stellar.anchor.util.StringHelper.isEmpty;

import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.stellar.anchor.config.SecretConfig;

@Data
public class PropertyDataConfig implements Validator {
  public static final String ERROR_SECRET_DATA_USERNAME_EMPTY = "secret-data-username-empty";
  public static final String ERROR_SECRET_DATA_PASSWORD_EMPTY = "secret-data-password-empty";
  String type;
  String server;
  String database;
  int initialConnectionPoolSize;
  int max_active_connections;
  boolean flywayEnabled;
  boolean dllAuto;
  String flywayLocation;
  private SecretConfig secretConfig;

  public PropertyDataConfig(SecretConfig secretConfig) {
    this.secretConfig = secretConfig;
  }

  @Override
  public boolean supports(@NotNull Class<?> clazz) {
    return PropertyEventConfig.class.isAssignableFrom(clazz);
  }

  @Override
  public void validate(@NotNull Object target, @NotNull Errors errors) {
    validateSecrets(errors);
  }

  private void validateSecrets(Errors errors) {
    switch (type) {
      case DATABASE_H2:
        break;
      case DATABASE_POSTGRES:
      case DATABASE_SQLITE:
      case DATABASE_AURORA:
        if (isEmpty(secretConfig.getDataSourceUsername())) {
          errors.reject(
              ERROR_SECRET_DATA_USERNAME_EMPTY, SECRET_DATA_USERNAME + " must not be empty.");
        }
        if (isEmpty(secretConfig.getDataSourcePassword())) {
          errors.reject(
              ERROR_SECRET_DATA_PASSWORD_EMPTY, SECRET_DATA_PASSWORD + " must not be empty.");
        }
        break;
    }
  }
}

package org.stellar.anchor.platform.config;

import static org.stellar.anchor.config.IntegrationAuthConfig.AuthType.*;

import java.util.List;
import lombok.Data;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.stellar.anchor.config.IntegrationAuthConfig;

@Data
public class PropertyIntegrationAuthConfig implements IntegrationAuthConfig, Validator {
  private static long minExpiration = 5000;

  AuthType authType = NONE;
  String platformToAnchorSecret;
  String anchorToPlatformSecret;
  Long expirationMilliseconds;

  @Override
  public boolean supports(Class<?> clazz) {
    return IntegrationAuthConfig.class.isAssignableFrom(clazz);
  }

  @Override
  public void validate(Object target, Errors errors) {
    IntegrationAuthConfig config = (IntegrationAuthConfig) target;

    if (List.of(API_KEY, JWT_TOKEN).contains(config.getAuthType())) {
      if (config.getPlatformToAnchorSecret() == null) {
        errors.rejectValue(
            "platformToAnchorSecret",
            "empty-platformToAnchorSecret",
            "platformToAnchorSecret cannot be empty for authType " + config.getAuthType());
      } else if (config.getAnchorToPlatformSecret() == null) {
        errors.rejectValue(
            "anchorToPlatformSecret",
            "empty-anchorToPlatformSecret",
            "anchorToPlatformSecret cannot be empty for authType " + config.getAuthType());
      }

      if (JWT_TOKEN == config.getAuthType()
          && (config.getExpirationMilliseconds() < minExpiration)) {
        errors.rejectValue(
            "expirationMilliseconds",
            "min-expirationMilliseconds",
            "expirationMilliseconds cannot be lower than " + minExpiration);
      }
    }
  }
}

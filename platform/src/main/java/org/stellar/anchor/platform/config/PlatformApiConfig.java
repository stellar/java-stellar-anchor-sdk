package org.stellar.anchor.platform.config;

import static org.stellar.anchor.auth.AuthType.API_KEY;
import static org.stellar.anchor.auth.AuthType.JWT;

import java.util.List;
import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.stellar.anchor.auth.AuthConfig;

@Data
public class PlatformApiConfig implements Validator {
  String baseUrl;
  AuthConfig auth;
  PropertySecretConfig secretConfig;

  public PlatformApiConfig(PropertySecretConfig secretConfig) {
    this.secretConfig = secretConfig;
  }

  public void setAuth(AuthConfig auth) {
    auth.setSecret(secretConfig.getPlatformApiSecret());
    this.auth = auth;
  }

  @Override
  public boolean supports(@NotNull Class<?> clazz) {
    return PlatformApiConfig.class.isAssignableFrom(clazz);
  }

  @Override
  public void validate(@NotNull Object target, @NotNull Errors errors) {
    PlatformApiConfig config = (PlatformApiConfig) target;
    if (List.of(API_KEY, JWT).contains(config.getAuth().getType())) {
      if (config.getAuth().getSecret() == null) {
        errors.rejectValue(
            "secret",
            "empty-secret",
            "Please set environment variable [platform_api.auth.secret] for auth type:"
                + config.getAuth().getType());
      }
    }
  }
}

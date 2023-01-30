package org.stellar.anchor.platform.config;

import static org.stellar.anchor.util.StringHelper.isEmpty;

import java.util.List;
import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.stellar.anchor.auth.AuthConfig;
import org.stellar.anchor.auth.AuthType;
import org.stellar.anchor.util.NetUtil;

@Data
public class CallbackApiConfig implements Validator {
  private static long MIN_EXPIRATION = 5000;

  String baseUrl;

  Boolean checkCertificate;

  AuthConfig auth;
  PropertySecretConfig secretConfig;

  public CallbackApiConfig(PropertySecretConfig secretConfig) {
    this.secretConfig = secretConfig;
  }

  public void setAuth(AuthConfig auth) {
    auth.setSecret(secretConfig.getCallbackApiSecret());
    this.auth = auth;
  }

  @Override
  public boolean supports(@NotNull Class<?> clazz) {
    return CallbackApiConfig.class.isAssignableFrom(clazz);
  }

  @Override
  public void validate(@NotNull Object target, @NotNull Errors errors) {
    CallbackApiConfig config = (CallbackApiConfig) target;
    validateBaseUrl(config, errors);
    validateAuth(config, errors);
  }

  void validateBaseUrl(CallbackApiConfig config, Errors errors) {
    if (isEmpty(config.baseUrl)) {
      errors.rejectValue(
          "baseUrl",
          "empty-callback-api-base-url",
          "The callback_api.base_url cannot be empty and must be defined");
    }
    if (!NetUtil.isUrlValid(config.baseUrl)) {
      errors.rejectValue(
          "baseUrl",
          "mal-formatted-callback-api-base-url",
          "The callback_api.base_url is not a valid URL");
    }
  }

  void validateAuth(CallbackApiConfig config, Errors errors) {
    if (List.of(AuthType.API_KEY, AuthType.JWT).contains(config.getAuth().getType())) {
      if (isEmpty(config.getAuth().getSecret())) {
        errors.rejectValue(
            "auth",
            "empty-secret-callback-api-secret",
            "Please set environment variable [SECRET.CALLBACK_API.AUTH_SECRET] for auth type:"
                + config.getAuth().getType());
      }

      if (AuthType.JWT == config.getAuth().getType()
          && (Long.parseLong(config.getAuth().getJwt().getExpirationMilliseconds())
              < MIN_EXPIRATION)) {
        errors.rejectValue(
            "auth",
            "min-expirationMilliseconds",
            "expirationMilliseconds cannot be lower than " + MIN_EXPIRATION);
      }
    }
  }
}

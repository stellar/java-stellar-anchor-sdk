package org.stellar.anchor.platform.config;

import java.util.List;
import lombok.Data;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.stellar.anchor.auth.AuthInfo;
import org.stellar.anchor.auth.AuthType;

@Data
public class CallbackApiConfig implements Validator {
  private static long MIN_EXPIRATION = 5000;

  String baseUrl;

  AuthInfo auth;
  PropertySecretConfig secretConfig;

  public CallbackApiConfig(PropertySecretConfig secretConfig) {
    this.secretConfig = secretConfig;
  }

  public void setAuth(AuthInfo auth) {
    auth.setSecret(secretConfig.getCallbackApiSecret());
    this.auth = auth;
  }

  @Override
  public boolean supports(Class<?> clazz) {
    return CallbackApiConfig.class.isAssignableFrom(clazz);
  }

  @Override
  public void validate(Object target, Errors errors) {
    CallbackApiConfig config = (CallbackApiConfig) target;

    if (List.of(AuthType.API_KEY, AuthType.JWT_TOKEN).contains(config.getAuth().getType())) {
      if (config.getAuth().getSecret() == null) {
        errors.rejectValue(
            "platformToAnchorSecret",
            "empty-platformToAnchorSecret",
            "Please set environment variable [SECRET.CALLBACK_API.AUTH_SECRET] for auth type:"
                + config.getAuth().getType());
      }

      if (AuthType.JWT_TOKEN == config.getAuth().getType()
          && (Long.parseLong(config.getAuth().getExpirationMilliseconds()) < MIN_EXPIRATION)) {
        errors.rejectValue(
            "expirationMilliseconds",
            "min-expirationMilliseconds",
            "expirationMilliseconds cannot be lower than " + MIN_EXPIRATION);
      }
    }
  }
}

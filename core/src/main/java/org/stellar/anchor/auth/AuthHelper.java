package org.stellar.anchor.auth;

import java.util.Calendar;
import javax.annotation.Nullable;
import org.stellar.anchor.api.exception.InvalidConfigException;
import org.stellar.anchor.util.AuthHeader;
import org.stellar.anchor.util.Log;

public class AuthHelper {
  public final AuthType authType;
  private JwtService jwtService;
  private long jwtExpirationMilliseconds;
  private String apiKey;

  private AuthHelper(AuthType authType) {
    this.authType = authType;
  }

  public static AuthHelper from(AuthType type, String secret, long jwtExpirationMilliseconds) {
    Log.info("AuthHelper.from: type=" + type + ", secret=" + secret);
    switch (type) {
      case JWT:
        // TODO: should this really be passing in the secret for both callback and platform?
        return AuthHelper.forJwtToken(
            new JwtService(null, null, null, secret, secret), jwtExpirationMilliseconds);
      case API_KEY:
        return AuthHelper.forApiKey(secret);
      default:
        return AuthHelper.forNone();
    }
  }

  public static AuthHelper forJwtToken(JwtService jwtService, long jwtExpirationMilliseconds) {
    AuthHelper authHelper = new AuthHelper(AuthType.JWT);
    authHelper.jwtService = jwtService;
    authHelper.jwtExpirationMilliseconds = jwtExpirationMilliseconds;
    return authHelper;
  }

  public static AuthHelper forApiKey(String apiKey) {
    AuthHelper authHelper = new AuthHelper(AuthType.API_KEY);
    authHelper.apiKey = apiKey;
    return authHelper;
  }

  public static AuthHelper forNone() {
    return new AuthHelper(AuthType.NONE);
  }

  @Nullable
  public AuthHeader<String, String> createAuthHeader() {
    Log.info("AuthHelper.createAuthHeader: authType=" + authType);
    switch (authType) {
      case JWT:
        long issuedAt = Calendar.getInstance().getTimeInMillis() / 1000L;
        long expirationTime = issuedAt + (jwtExpirationMilliseconds / 1000L);
        // TODO: why aren't we handling SEP-10 tokens explicitly?
        ApiAuthJwt token = new ApiAuthJwt(issuedAt, expirationTime);
        try {
          return new AuthHeader<>("Authorization", "Bearer " + jwtService.encode(token));
        } catch (InvalidConfigException e) {
          Log.error("Error creating auth header", e);
          return null;
        }

      case API_KEY:
        return new AuthHeader<>("X-Api-Key", apiKey);

      default:
        return null;
    }
  }
}

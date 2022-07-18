package org.stellar.anchor.auth;

import java.util.Calendar;
import javax.annotation.Nullable;
import org.stellar.anchor.config.IntegrationAuthConfig.AuthType;
import org.stellar.anchor.util.AuthHeader;

public class AuthHelper {
  public final AuthType authType;
  private JwtService jwtService;
  private long jwtExpirationMilliseconds;
  private String issuerUrl;
  private String apiKey;

  private AuthHelper(AuthType authType) {
    this.authType = authType;
  }

  public static AuthHelper forJwtToken(
      JwtService jwtService, long jwtExpirationMilliseconds, String issuerUrl) {
    AuthHelper authHelper = new AuthHelper(AuthType.JWT_TOKEN);
    authHelper.jwtService = jwtService;
    authHelper.issuerUrl = issuerUrl;
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
    switch (authType) {
      case JWT_TOKEN:
        long issuedAt = Calendar.getInstance().getTimeInMillis() / 1000L;
        long expirationTime = issuedAt + (jwtExpirationMilliseconds / 1000L);
        JwtToken token = JwtToken.of(issuerUrl, issuedAt, expirationTime);
        return new AuthHeader<>("Authorization", "Bearer " + jwtService.encode(token));

      case API_KEY:
        return new AuthHeader<>("X-Api-Key", apiKey);

      default:
        return null;
    }
  }
}

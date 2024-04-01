package org.stellar.anchor.auth;

import javax.annotation.Nullable;
import javax.crypto.SecretKey;
import lombok.*;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class AuthConfig {
  AuthType type;
  String secretString;
  @Nullable // Only not-null when type is JWT
  SecretKey secretJwt;
  JwtConfig jwt;
  ApiKeyConfig apiKey;

  @Setter
  @Getter
  @AllArgsConstructor
  @NoArgsConstructor
  public static class JwtConfig {
    String expirationMilliseconds;
    String httpHeader;
  }

  @Setter
  @Getter
  @AllArgsConstructor
  @NoArgsConstructor
  public static class ApiKeyConfig {
    String httpHeader;
  }
}

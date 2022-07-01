package org.stellar.anchor.auth;

import java.util.Calendar;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class AuthHelper {
  private final JwtService jwtService;
  private final long jwtExpirationMilliseconds;
  private final String issuerUrl;

  public String createJwtToken() {
    long issuedAt = Calendar.getInstance().getTimeInMillis() / 1000L;
    long expirationTime = issuedAt + (jwtExpirationMilliseconds / 1000L);
    JwtToken token = JwtToken.of(issuerUrl, issuedAt, expirationTime);
    return jwtService.encode(token);
  }
}

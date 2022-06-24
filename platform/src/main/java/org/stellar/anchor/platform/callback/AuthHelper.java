package org.stellar.anchor.platform.callback;

import java.util.Calendar;
import lombok.AllArgsConstructor;
import org.stellar.anchor.sep10.JwtService;
import org.stellar.anchor.sep10.JwtToken;

@AllArgsConstructor
public class AuthHelper {
  private final JwtService jwtService;
  private final long jwtExpirationMilliseconds;
  private final String issuerUrl;

  String createJwtToken() {
    long issuedAt = Calendar.getInstance().getTimeInMillis() / 1000L;
    long expirationTime = issuedAt + (jwtExpirationMilliseconds / 1000L);
    JwtToken token = JwtToken.of(issuerUrl, issuedAt, expirationTime);
    return jwtService.encode(token);
  }
}

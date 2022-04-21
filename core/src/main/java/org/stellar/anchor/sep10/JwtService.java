package org.stellar.anchor.sep10;

import io.jsonwebtoken.*;
import io.jsonwebtoken.impl.DefaultJwsHeader;
import org.apache.commons.codec.binary.Base64;
import org.stellar.anchor.config.AppConfig;

import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.Map;

public class JwtService {
  final String jwtKey;

  public JwtService(AppConfig appConfig) {
    this(appConfig.getJwtSecretKey());
  }

  public JwtService(String secretKey) {
    this.jwtKey = Base64.encodeBase64String(secretKey.getBytes(StandardCharsets.UTF_8));
  }

  public String encode(JwtToken token) {
    Calendar calIat = Calendar.getInstance();
    calIat.setTimeInMillis(1000L * token.getIat());

    Calendar calExp = Calendar.getInstance();
    calExp.setTimeInMillis(1000L * token.getExp());

    JwtBuilder builder =
        Jwts.builder()
            .setId(token.getJti())
            .setIssuer(token.getIss())
            .setSubject(token.getSub())
            .setIssuedAt(calIat.getTime())
            .setExpiration(calExp.getTime())
            .setSubject(token.sub);

    if (token.clientDomain != null) {
      builder.addClaims(Map.of("client_domain", token.clientDomain));
    }

    return builder.signWith(SignatureAlgorithm.HS256, jwtKey).compact();
  }

  @SuppressWarnings("rawtypes")
  public JwtToken decode(String cipher) {
    JwtParser jwtParser = Jwts.parser();
    jwtParser.setSigningKey(jwtKey);
    Jwt jwt = jwtParser.parse(cipher);
    Header header = jwt.getHeader();
    if (!(header instanceof DefaultJwsHeader)) {
      // This should not happen
      throw new IllegalArgumentException("Bad token");
    }
    DefaultJwsHeader defaultHeader = (DefaultJwsHeader) header;
    if (!defaultHeader.getAlgorithm().equals(SignatureAlgorithm.HS256.getValue())) {
      // Not signed by the JWTService.
      throw new IllegalArgumentException("Bad token");
    }
    Claims claims = (Claims) jwt.getBody();
    return JwtToken.of(
        (String) claims.get("iss"),
        (String) claims.get("sub"),
        Long.valueOf((Integer) claims.get("iat")),
        Long.valueOf((Integer) claims.get("exp")),
        (String) claims.get("jti"),
        (String) claims.get("client_domain"));
  }
}

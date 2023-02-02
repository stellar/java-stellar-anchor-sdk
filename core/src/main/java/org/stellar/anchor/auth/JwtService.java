package org.stellar.anchor.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;
import io.jsonwebtoken.*;
import io.jsonwebtoken.impl.DefaultJwsHeader;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import lombok.Data;
import lombok.Getter;
import org.apache.commons.codec.binary.Base64;
import org.stellar.anchor.api.exception.InvalidConfigException;
import org.stellar.anchor.config.SecretConfig;

@Getter
public class JwtService {
  public static final String CLIENT_DOMAIN = "client_domain";
  String sep10JwtSecret;
  String sep24InteractiveUrlJwtSecret;
  String sep24MoreInfoUrlJwtSecret;

  public JwtService(SecretConfig secretConfig) {
    this(
        secretConfig.getSep10JwtSecretKey(),
        secretConfig.getSep24InteractiveUrlJwtSecret(),
        secretConfig.getSep24MoreInfoUrlJwtSecret());
  }

  public JwtService(
      String sep10JwtSecret,
      String sep24InteractiveUrlJwtSecret,
      String sep24MoreInfoUrlJwtSecret) {
    this.sep10JwtSecret =
        (sep10JwtSecret == null)
            ? null
            : Base64.encodeBase64String(sep10JwtSecret.getBytes(StandardCharsets.UTF_8));
    this.sep24InteractiveUrlJwtSecret =
        (sep24InteractiveUrlJwtSecret == null)
            ? null
            : Base64.encodeBase64String(
                sep24InteractiveUrlJwtSecret.getBytes(StandardCharsets.UTF_8));
    this.sep24MoreInfoUrlJwtSecret = sep24MoreInfoUrlJwtSecret;
  }

  public String encode(Sep10Jwt token) {
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
            .setSubject(token.getSub());

    if (token.getClientDomain() != null) {
      builder.claim(CLIENT_DOMAIN, token.getClientDomain());
    }

    return builder.signWith(SignatureAlgorithm.HS256, sep10JwtSecret).compact();
  }

  public String encode(Sep24InteractiveUrlJwt token) throws InvalidConfigException {
    if (sep24InteractiveUrlJwtSecret == null) {
      throw new InvalidConfigException(
          "Please provide the secret before encoding JWT for Sep24 interactive url");
    }
    Calendar calExp = Calendar.getInstance();
    calExp.setTimeInMillis(1000L * token.getExp());
    JwtBuilder builder = Jwts.builder().setId(token.getJti()).setExpiration(calExp.getTime());
    for (Map.Entry<String, Object> claim : token.claims.entrySet()) {
      builder.claim(claim.getKey(), claim.getValue());
    }

    return builder.signWith(SignatureAlgorithm.HS256, sep10JwtSecret).compact();
  }

  @SuppressWarnings("rawtypes")
  public Jwt decode(String cipher) {
    JwtParser jwtParser = Jwts.parser();
    jwtParser.setSigningKey(sep10JwtSecret);
    Jwt jwt = jwtParser.parseClaimsJws(cipher);
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

    return jwt;
  }
}

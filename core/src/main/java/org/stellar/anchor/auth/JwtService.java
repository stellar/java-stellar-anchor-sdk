package org.stellar.anchor.auth;

import io.jsonwebtoken.*;
import io.jsonwebtoken.impl.DefaultJwsHeader;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.Map;
import lombok.Getter;
import org.apache.commons.codec.binary.Base64;
import org.stellar.anchor.api.exception.InvalidConfigException;
import org.stellar.anchor.api.exception.NotSupportedException;
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
    this.sep24MoreInfoUrlJwtSecret =
        (sep24MoreInfoUrlJwtSecret == null)
            ? null
            : Base64.encodeBase64String(sep24MoreInfoUrlJwtSecret.getBytes(StandardCharsets.UTF_8));
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

    return builder.signWith(SignatureAlgorithm.HS256, sep24InteractiveUrlJwtSecret).compact();
  }

  public String encode(Sep24MoreInfoUrlJwt token) throws InvalidConfigException {
    if (sep24MoreInfoUrlJwtSecret == null) {
      throw new InvalidConfigException(
          "Please provide the secret before encoding JWT for more_info_url");
    }
    Calendar calExp = Calendar.getInstance();
    calExp.setTimeInMillis(1000L * token.getExp());
    JwtBuilder builder = Jwts.builder().setId(token.getJti()).setExpiration(calExp.getTime());
    for (Map.Entry<String, Object> claim : token.claims.entrySet()) {
      builder.claim(claim.getKey(), claim.getValue());
    }

    return builder.signWith(SignatureAlgorithm.HS256, sep24MoreInfoUrlJwtSecret).compact();
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  public <T extends AbstractJwt> T decode(String cipher, Class<T> cls)
      throws NotSupportedException, NoSuchMethodException, InvocationTargetException,
          InstantiationException, IllegalAccessException {
    String secret;
    if (cls.equals(Sep10Jwt.class)) {
      secret = sep10JwtSecret;
    } else if (cls.equals(Sep24InteractiveUrlJwt.class)) {
      secret = sep24InteractiveUrlJwtSecret;
    } else if (cls.equals(Sep24MoreInfoUrlJwt.class)) {
      secret = sep24MoreInfoUrlJwtSecret;
    } else {
      throw new NotSupportedException(
          String.format("The Jwt class:[%s] is not supported", cls.getName()));
    }

    JwtParser jwtParser = Jwts.parser();
    jwtParser.setSigningKey(secret);
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

    if (cls.equals(Sep10Jwt.class)) {
      return (T) Sep10Jwt.class.getConstructor(Jwt.class).newInstance(jwt);
    } else if (cls.equals(Sep24InteractiveUrlJwt.class)) {
      return (T) Sep24InteractiveUrlJwt.class.getConstructor(Jwt.class).newInstance(jwt);
    } else {
      return (T) Sep24MoreInfoUrlJwt.class.getConstructor(Jwt.class).newInstance(jwt);
    }
  }
}

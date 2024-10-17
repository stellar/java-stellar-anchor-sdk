package org.stellar.anchor.util;

import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.JwtParserBuilder;
import io.jsonwebtoken.Jwts;
import org.stellar.anchor.auth.JwtsGsonDeserializer;
import org.stellar.anchor.auth.JwtsGsonSerializer;

public final class JwtUtil {
  public static JwtBuilder jwtsBuilder() {
    return Jwts.builder().json(JwtsGsonSerializer.newInstance());
  }

  public static JwtParserBuilder jwtsParser() {
    return Jwts.parser().json(JwtsGsonDeserializer.newInstance());
  }
}

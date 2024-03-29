package org.stellar.anchor.util;

import static org.stellar.anchor.util.StringHelper.isEmpty;

import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;

public final class KeyUtil {
  public static SecretKey toSecretKeySpecOrNull(String secret) {
    return isEmpty(secret) ? null : Keys.hmacShaKeyFor(((secret.getBytes(StandardCharsets.UTF_8))));
  }
}

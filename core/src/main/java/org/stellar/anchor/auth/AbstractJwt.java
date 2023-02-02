package org.stellar.anchor.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwt;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.stellar.anchor.api.exception.SepException;

@Getter
@Setter
@NoArgsConstructor
public abstract class AbstractJwt {
  static final Set<String> claimExclusion =
      Stream.of("jti", "iss", "sub", "iat", "exp").collect(Collectors.toCollection(HashSet::new));
  String jti; // JWT ID
  String iss; // Issuer
  String sub; // Subject
  long iat; // Issued At
  long exp; // Expiration Time

  Map<String, Object> claims = new HashMap<>();

  public AbstractJwt(Jwt jwt) {
    Claims claims = (Claims) jwt.getBody();
    if (claims.get("jti") != null) jti = (String) claims.get("jti");
    if (claims.get("iss") != null) iss = (String) claims.get("iss");
    if (claims.get("sub") != null) sub = (String) claims.get("sub");
    if (claims.get("iat") != null) iat = Long.parseLong(claims.get("iat").toString());
    if (claims.get("exp") != null) exp = Long.parseLong(claims.get("exp").toString());

    for (Map.Entry<String, Object> e : claims.entrySet()) {
      if (!claimExclusion.contains(e.getKey())) {
        this.claims.put(e.getKey(), e.getValue());
      }
    }
  }

  public void claim(String claim, Object value) throws SepException {
    if (claims.containsKey(claim)) {
      throw new SepException(String.format("Claim [%s] is already added.", claim));
    }
    claims.put(claim, value);
  }

  public Map<String, Object> claims() {
    return claims;
  }
}

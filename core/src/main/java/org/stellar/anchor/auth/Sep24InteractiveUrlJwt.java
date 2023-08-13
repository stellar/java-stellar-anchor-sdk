package org.stellar.anchor.auth;

import static org.stellar.anchor.auth.JwtService.CLIENT_DOMAIN;
import static org.stellar.anchor.auth.JwtService.CLIENT_NAME;

import io.jsonwebtoken.Jwt;
import org.stellar.anchor.api.exception.SepException;

public class Sep24InteractiveUrlJwt extends AbstractJwt {
  public Sep24InteractiveUrlJwt(
      String sub, String jti, long exp, String clientDomain, String clientName)
      throws SepException {
    super.sub = sub;
    super.jti = jti;
    super.exp = exp;
    super.claim(CLIENT_DOMAIN, clientDomain);
    super.claim(CLIENT_NAME, clientName);
  }

  public Sep24InteractiveUrlJwt(Jwt jwt) {
    super(jwt);
  }
}

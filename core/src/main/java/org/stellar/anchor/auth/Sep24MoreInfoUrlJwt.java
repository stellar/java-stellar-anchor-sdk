package org.stellar.anchor.auth;

import io.jsonwebtoken.Jwt;

public class Sep24MoreInfoUrlJwt extends AbstractJwt {
  public Sep24MoreInfoUrlJwt(String jti, long exp) {
    super.jti = jti;
    super.exp = exp;
  }

  public Sep24MoreInfoUrlJwt(Jwt jwt) {
    super(jwt);
  }
}

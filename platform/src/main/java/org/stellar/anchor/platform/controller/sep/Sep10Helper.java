package org.stellar.anchor.platform.controller.sep;

import static org.stellar.anchor.filter.Sep10JwtFilter.JWT_TOKEN;

import javax.servlet.http.HttpServletRequest;
import org.stellar.anchor.api.exception.SepValidationException;
import org.stellar.anchor.auth.Sep10Jwt;

public class Sep10Helper {
  public static Sep10Jwt getSep10Token(HttpServletRequest request) throws SepValidationException {
    Sep10Jwt token = (Sep10Jwt) request.getAttribute(JWT_TOKEN);
    if (token == null) {
      throw new SepValidationException(
          "missing sep10 jwt token. Make sure the sep10 filter is invoked before the controller");
    }
    return token;
  }
}

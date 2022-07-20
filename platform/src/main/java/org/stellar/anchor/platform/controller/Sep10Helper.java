package org.stellar.anchor.platform.controller;

import static org.stellar.anchor.filter.JwtTokenFilter.JWT_TOKEN;

import javax.servlet.http.HttpServletRequest;
import org.stellar.anchor.api.exception.SepValidationException;
import org.stellar.anchor.auth.JwtToken;

public class Sep10Helper {
  public static JwtToken getSep10Token(HttpServletRequest request) throws SepValidationException {
    JwtToken token = (JwtToken) request.getAttribute(JWT_TOKEN);
    if (token == null) {
      throw new SepValidationException(
          "missing sep10 jwt token. Make sure the sep10 filter is invoked before the controller");
    }
    return token;
  }
}

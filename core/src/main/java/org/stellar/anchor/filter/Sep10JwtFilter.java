package org.stellar.anchor.filter;

import static org.stellar.anchor.util.Log.*;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import org.stellar.anchor.api.exception.SepValidationException;
import org.stellar.anchor.auth.JwtService;
import org.stellar.anchor.auth.Sep10Jwt;

public class Sep10JwtFilter extends AbstractJwtFilter {
  public Sep10JwtFilter(JwtService jwtService) {
    super(jwtService);
  }

  @Override
  void check(String jwtCipher, HttpServletRequest request, ServletResponse servletResponse)
      throws Exception {
    Sep10Jwt token = jwtService.decode(jwtCipher, Sep10Jwt.class);
    if (token == null) {
      throw new SepValidationException("JwtToken should not be null");
    }
    infoF("token created. account={} url={}", shorter(token.getAccount()), request.getRequestURL());
    debug(String.format("storing token to request %s:", request.getRequestURL()), token);
    request.setAttribute(JWT_TOKEN, token);
  }
}

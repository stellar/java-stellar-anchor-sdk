package org.stellar.anchor.filter;

import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import org.stellar.anchor.auth.ApiAuthJwt.PlatformAuthJwt;
import org.stellar.anchor.auth.JwtService;

public class PlatformAuthJwtFilter extends AbstractJwtFilter {
  public PlatformAuthJwtFilter(JwtService jwtService) {
    super(jwtService);
  }

  @Override
  void check(String jwtCipher, HttpServletRequest request, ServletResponse servletResponse)
      throws Exception {
    PlatformAuthJwt token = jwtService.decode(jwtCipher, PlatformAuthJwt.class);
    request.setAttribute(JWT_TOKEN, token);
  }
}

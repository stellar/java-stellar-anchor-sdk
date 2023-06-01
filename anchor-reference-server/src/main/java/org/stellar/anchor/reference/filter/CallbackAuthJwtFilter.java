package org.stellar.anchor.reference.filter;

import java.util.Calendar;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import lombok.NonNull;
import org.stellar.anchor.auth.ApiAuthJwt;
import org.stellar.anchor.auth.JwtService;
import org.stellar.anchor.filter.AbstractJwtFilter;

public class CallbackAuthJwtFilter extends AbstractJwtFilter {
  public CallbackAuthJwtFilter(JwtService jwtService) {
    super(jwtService);
  }

  @Override
  public void check(String jwtCipher, HttpServletRequest request, ServletResponse servletResponse)
      throws Exception {
    @NonNull
    ApiAuthJwt.CallbackAuthJwt token =
        jwtService.decode(jwtCipher, ApiAuthJwt.CallbackAuthJwt.class);

    long currentTime = Calendar.getInstance().getTimeInMillis() / 1000;
    if (token.getExp() <= currentTime) {
      throw new IllegalArgumentException("Token expired");
    }

    request.setAttribute(JWT_TOKEN, token);
  }
}

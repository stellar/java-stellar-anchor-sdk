package org.stellar.anchor.filter;

import static org.stellar.anchor.util.Log.*;

import com.google.gson.Gson;
import java.io.IOException;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.SneakyThrows;
import org.apache.http.HttpStatus;
import org.stellar.anchor.api.exception.SepValidationException;
import org.stellar.anchor.api.sep.SepExceptionResponse;
import org.stellar.anchor.auth.JwtService;
import org.stellar.anchor.auth.JwtToken;
import org.stellar.anchor.util.GsonUtils;
import org.stellar.anchor.util.Log;

public class JwtTokenFilter implements Filter {
  public static final String JWT_TOKEN = "token";
  private static final String OPTIONS = "OPTIONS";
  public static final String APPLICATION_JSON_VALUE = "application/json";
  private static final Gson gson = GsonUtils.builder().setPrettyPrinting().create();
  private final JwtService jwtService;

  public JwtTokenFilter(JwtService jwtService) {
    this.jwtService = jwtService;
  }

  @Override
  public void init(FilterConfig filterConfig) {}

  @SneakyThrows
  @Override
  public void doFilter(
      ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) {
    if (!(servletRequest instanceof HttpServletRequest)) {
      throw new ServletException("the request must be a HttpServletRequest");
    }

    if (!(servletResponse instanceof HttpServletResponse)) {
      throw new ServletException("the request must be a HttpServletRequest");
    }

    HttpServletRequest request = (HttpServletRequest) servletRequest;
    HttpServletResponse response = (HttpServletResponse) servletResponse;
    Log.infoF(
        "Applying JwtTokenFilter on request {}?{}",
        request.getRequestURL().toString(),
        request.getQueryString());

    if (request.getMethod().equals(OPTIONS)) {
      filterChain.doFilter(servletRequest, servletResponse);
      return;
    }

    String authorization = request.getHeader("Authorization");
    if (authorization == null) {
      sendForbiddenError(response);
      return;
    }

    if (!authorization.contains("Bearer")) {
      sendForbiddenError(response);
      return;
    }

    String jwtCipher;
    try {
      jwtCipher = authorization.split(" ")[1];
    } catch (Exception ex) {
      sendForbiddenError(response);
      return;
    }

    try {
      JwtToken token = jwtService.decode(jwtCipher);
      validate(token);
      infoF(
          "token created. account={} url={}", shorter(token.getAccount()), request.getRequestURL());
      debug(String.format("storing token to request %s:", request.getRequestURL()), token);
      request.setAttribute(JWT_TOKEN, token);
    } catch (Exception ex) {
      sendForbiddenError(response);
      return;
    }

    filterChain.doFilter(servletRequest, servletResponse);
  }

  protected void validate(JwtToken token) throws SepValidationException {
    if (token == null) {
      throw new SepValidationException("JwtToken should not be null");
    }
    // TODO: Add more validation.
  }

  private static void sendForbiddenError(HttpServletResponse response) throws IOException {
    error("Forbidden: JwtTokenFilter failed to authenticate the request.");
    response.setStatus(HttpStatus.SC_FORBIDDEN);
    response.setContentType(APPLICATION_JSON_VALUE);
    response.getWriter().print(gson.toJson(new SepExceptionResponse("forbidden")));
  }

  @Override
  public void destroy() {}
}

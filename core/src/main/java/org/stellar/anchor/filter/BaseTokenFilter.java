package org.stellar.anchor.filter;

import static org.stellar.anchor.util.Log.*;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.SneakyThrows;
import org.apache.http.HttpStatus;
import org.stellar.anchor.dto.SepExceptionResponse;
import org.stellar.anchor.exception.SepValidationException;
import org.stellar.anchor.sep10.JwtService;
import org.stellar.anchor.sep10.JwtToken;

public abstract class BaseTokenFilter implements Filter {
  public static final String JWT_TOKEN = "token";
  public static final String OPTIONS = "OPTIONS";
  public static final String APPLICATION_JSON_VALUE = "application/json";
  final Gson gson;
  final JwtService jwtService;

  public BaseTokenFilter(JwtService jwtService) {
    this.jwtService = jwtService;
    this.gson = new GsonBuilder().setPrettyPrinting().create();
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

  protected abstract void validate(JwtToken token) throws SepValidationException;

  private void sendForbiddenError(HttpServletResponse response) throws IOException {
    response.setStatus(HttpStatus.SC_FORBIDDEN);
    response.setContentType(APPLICATION_JSON_VALUE);
    response.getWriter().print(gson.toJson(new SepExceptionResponse("forbidden")));
  }

  protected abstract boolean isEnabled();

  @Override
  public void destroy() {}
}

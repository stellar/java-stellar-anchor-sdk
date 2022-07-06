package org.stellar.anchor.filter;

import static org.stellar.anchor.util.Log.*;

import com.google.gson.Gson;
import java.io.IOException;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.SneakyThrows;
import org.apache.http.HttpStatus;
import org.stellar.anchor.api.sep.SepExceptionResponse;
import org.stellar.anchor.util.GsonUtils;

public class ApiKeyFilter implements Filter {
  public static final String OPTIONS = "OPTIONS";
  public static final String APPLICATION_JSON_VALUE = "application/json";
  public static final String HEADER_NAME = "X-Api-Key";
  final Gson gson;
  final String apiKey;

  public ApiKeyFilter(String apiKey) {
    this.apiKey = apiKey;
    this.gson = GsonUtils.builder().setPrettyPrinting().create();
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

    String gotApiKey = request.getHeader(HEADER_NAME);
    if (gotApiKey == null) {
      sendForbiddenError(response);
      return;
    }

    if (!apiKey.equals(gotApiKey)) {
      sendForbiddenError(response);
      return;
    }

    infoF("apiKey auth passed for url={}", request.getRequestURL());

    filterChain.doFilter(servletRequest, servletResponse);
  }

  private void sendForbiddenError(HttpServletResponse response) throws IOException {
    response.setStatus(HttpStatus.SC_FORBIDDEN);
    response.setContentType(APPLICATION_JSON_VALUE);
    response.getWriter().print(gson.toJson(new SepExceptionResponse("forbidden")));
  }

  @Override
  public void destroy() {}
}

package org.stellar.anchor.filter;

import jakarta.servlet.*;
import java.io.IOException;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public class NoneFilter implements Filter {
  @Override
  public void init(FilterConfig filterConfig) {}

  @Override
  public void doFilter(
      ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
      throws IOException, ServletException {
    filterChain.doFilter(servletRequest, servletResponse);
  }

  @Override
  public void destroy() {}
}

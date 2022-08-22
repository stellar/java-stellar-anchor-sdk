package org.stellar.anchor.platform;

import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.stellar.anchor.platform.utils.LoggableDispatcherServlet;

@Configuration
public class RequestLoggableConfig {
  @Bean
  public ServletRegistrationBean<LoggableDispatcherServlet> dispatcherRegistration(
      LoggableDispatcherServlet dispatcherServlet) {
    return new ServletRegistrationBean<>(dispatcherServlet);
  }

  @Bean(name = DispatcherServletAutoConfiguration.DEFAULT_DISPATCHER_SERVLET_BEAN_NAME)
  public LoggableDispatcherServlet dispatcherServlet() {
    return new LoggableDispatcherServlet();
  }
}

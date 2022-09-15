package org.stellar.anchor.platform;

import javax.servlet.Filter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.stellar.anchor.platform.utils.RequestLoggerFilter;

@Configuration
public class RequestLoggerConfig {
  @Bean
  public FilterRegistrationBean<Filter> requestLoggerFilter() {
    FilterRegistrationBean<Filter> registrationBean = new FilterRegistrationBean<>();
    registrationBean.setFilter(new RequestLoggerFilter());
    registrationBean.addUrlPatterns("/");
    registrationBean.addUrlPatterns("/*");
    return registrationBean;
  }
}

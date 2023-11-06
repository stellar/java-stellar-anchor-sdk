package org.stellar.anchor.client.component.share;

import javax.servlet.Filter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.stellar.anchor.client.config.AppLoggingConfig;
import org.stellar.anchor.client.utils.RequestLoggerFilter;

@Configuration
public class RequestLoggerBeans {
  @Bean
  public FilterRegistrationBean<Filter> requestLoggerFilter(AppLoggingConfig appLoggingConfig) {
    FilterRegistrationBean<Filter> registrationBean = new FilterRegistrationBean<>();
    registrationBean.setFilter(new RequestLoggerFilter(appLoggingConfig));
    registrationBean.addUrlPatterns("/");
    registrationBean.addUrlPatterns("/*");
    return registrationBean;
  }
}

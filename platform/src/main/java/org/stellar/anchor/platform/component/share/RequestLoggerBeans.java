package org.stellar.anchor.platform.component.share;

import javax.servlet.Filter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.stellar.anchor.platform.config.AppLoggingConfig;
import org.stellar.anchor.platform.utils.RequestLoggerFilter;

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

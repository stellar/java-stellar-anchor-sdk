package org.stellar.anchor.platform;

import javax.servlet.Filter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.stellar.anchor.auth.JwtService;
import org.stellar.anchor.filter.ApiKeyFilter;
import org.stellar.anchor.filter.JwtTokenFilter;
import org.stellar.anchor.filter.NoneFilter;
import org.stellar.anchor.platform.config.PlatformApiConfig;

@Configuration
public class PlatformApiBeans {
  /**
   * Register anchor-to-platform token filter.
   *
   * @return Spring Filter Registration Bean
   */
  @Bean
  public FilterRegistrationBean<Filter> anchorToPlatformTokenFilter(
      PlatformApiConfig platformApiConfig) {
    Filter anchorToPlatformFilter;
    String authSecret = platformApiConfig.getAuth().getSecret();
    switch (platformApiConfig.getAuth().getType()) {
      case JWT_TOKEN:
        JwtService jwtService = new JwtService(authSecret);
        anchorToPlatformFilter = new JwtTokenFilter(jwtService);
        break;

      case API_KEY:
        anchorToPlatformFilter = new ApiKeyFilter(authSecret);
        break;

      default:
        anchorToPlatformFilter = new NoneFilter();
        break;
    }

    FilterRegistrationBean<Filter> registrationBean = new FilterRegistrationBean<>();
    registrationBean.setFilter(anchorToPlatformFilter);
    registrationBean.addUrlPatterns("/transactions/*");
    registrationBean.addUrlPatterns("/transactions");
    registrationBean.addUrlPatterns("/exchange/quotes/*");
    registrationBean.addUrlPatterns("/exchange/quotes");
    return registrationBean;
  }
}

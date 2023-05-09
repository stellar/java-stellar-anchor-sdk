package org.stellar.anchor.platform.component.custody;

import javax.servlet.Filter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.stellar.anchor.auth.JwtService;
import org.stellar.anchor.filter.ApiKeyFilter;
import org.stellar.anchor.filter.JwtTokenFilter;
import org.stellar.anchor.filter.NoneFilter;
import org.stellar.anchor.platform.config.CustodyApiConfig;
import org.stellar.anchor.platform.custody.CustodyTransactionService;
import org.stellar.anchor.platform.custody.PaymentService;
import org.stellar.anchor.platform.data.JdbcCustodyTransactionRepo;

@Configuration
public class CustodyBeans {

  @Bean
  CustodyTransactionService getCustodyTransactionService(
      JdbcCustodyTransactionRepo custodyTransactionRepo, PaymentService paymentService) {
    return new CustodyTransactionService(custodyTransactionRepo, paymentService);
  }

  /**
   * Register platform-to-custody token filter.
   *
   * @return Spring Filter Registration Bean
   */
  @Bean
  public FilterRegistrationBean<Filter> platformToCustodyTokenFilter(
      CustodyApiConfig custodyApiConfig) {
    String authSecret = custodyApiConfig.getAuth().getSecret();

    Filter platformToCustody;
    switch (custodyApiConfig.getAuth().getType()) {
      case JWT:
        JwtService jwtService = new JwtService(authSecret, null, null);
        platformToCustody = new JwtTokenFilter(jwtService);
        break;

      case API_KEY:
        platformToCustody = new ApiKeyFilter(authSecret);
        break;

      default:
        platformToCustody = new NoneFilter();
        break;
    }

    FilterRegistrationBean<Filter> registrationBean = new FilterRegistrationBean<>();
    registrationBean.setFilter(platformToCustody);
    registrationBean.addUrlPatterns("/transactions/*");
    return registrationBean;
  }
}

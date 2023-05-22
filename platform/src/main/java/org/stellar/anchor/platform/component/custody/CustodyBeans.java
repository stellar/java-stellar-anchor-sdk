package org.stellar.anchor.platform.component.custody;

import java.util.concurrent.TimeUnit;
import javax.servlet.Filter;
import okhttp3.OkHttpClient;
import okhttp3.OkHttpClient.Builder;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.stellar.anchor.auth.JwtService;
import org.stellar.anchor.filter.ApiKeyFilter;
import org.stellar.anchor.filter.JwtTokenFilter;
import org.stellar.anchor.filter.NoneFilter;
import org.stellar.anchor.platform.config.CustodyApiConfig;
import org.stellar.anchor.platform.config.PropertyCustodyConfig;
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

  @Bean(name = "custodyHttpClient")
  OkHttpClient custodyHttpClient(PropertyCustodyConfig custodyConfig) {
    return new Builder()
        .connectTimeout(custodyConfig.getHttpClient().getConnectTimeout(), TimeUnit.SECONDS)
        .readTimeout(custodyConfig.getHttpClient().getReadTimeout(), TimeUnit.SECONDS)
        .writeTimeout(custodyConfig.getHttpClient().getWriteTimeout(), TimeUnit.SECONDS)
        .callTimeout(custodyConfig.getHttpClient().getCallTimeout(), TimeUnit.SECONDS)
        .build();
  }
}

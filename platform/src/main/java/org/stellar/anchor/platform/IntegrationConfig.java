package org.stellar.anchor.platform;

import okhttp3.OkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.stellar.anchor.config.Sep12Config;
import org.stellar.anchor.config.Sep38Config;
import org.stellar.anchor.integration.customer.CustomerIntegration;
import org.stellar.anchor.integration.rate.RateIntegration;
import org.stellar.anchor.util.OkHttpUtil;

@Configuration
public class IntegrationConfig {
  @Bean
  CustomerIntegration customerIntegration(Sep12Config sep12Config) {
    OkHttpClient httpClient = OkHttpUtil.buildClient();
    return new PlatformCustomerIntegration(
        sep12Config.getCustomerIntegrationEndPoint(), httpClient);
  }

  @Bean
  RateIntegration rateIntegration(Sep38Config sep38Config) {
    OkHttpClient httpClient = OkHttpUtil.buildClient();
    return new PlatformRateIntegration(sep38Config.getQuoteIntegrationEndPoint(), httpClient);
  }
}

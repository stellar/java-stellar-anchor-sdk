package org.stellar.anchor.platform;

import com.google.gson.Gson;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.stellar.anchor.config.Sep12Config;
import org.stellar.anchor.config.Sep38Config;
import org.stellar.anchor.integration.customer.CustomerIntegration;
import org.stellar.anchor.integration.rate.RateIntegration;

@Configuration
public class IntegrationConfig {
  @Bean
  OkHttpClient httpClient() {
    return new OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.MINUTES)
        .readTimeout(10, TimeUnit.MINUTES)
        .writeTimeout(10, TimeUnit.MINUTES)
        .callTimeout(10, TimeUnit.MINUTES)
        .build();
  }

  @Bean
  CustomerIntegration customerIntegration(
      Sep12Config sep12Config, OkHttpClient httpClient, Gson gson) {
    return new PlatformCustomerIntegration(
        sep12Config.getCustomerIntegrationEndPoint(), httpClient, gson);
  }

  @Bean
  RateIntegration rateIntegration(Sep38Config sep38Config, OkHttpClient httpClient, Gson gson) {
    return new PlatformRateIntegration(sep38Config.getQuoteIntegrationEndPoint(), httpClient, gson);
  }
}

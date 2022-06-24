package org.stellar.anchor.platform;

import com.google.gson.Gson;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.stellar.anchor.api.callback.CustomerIntegration;
import org.stellar.anchor.api.callback.FeeIntegration;
import org.stellar.anchor.api.callback.RateIntegration;
import org.stellar.anchor.config.*;
import org.stellar.anchor.platform.callback.RestCustomerIntegration;
import org.stellar.anchor.platform.callback.RestFeeIntegration;
import org.stellar.anchor.platform.callback.RestRateIntegration;
import org.stellar.anchor.sep10.JwtService;

@Configuration
public class IntegrationConfig {
  @Bean
  JwtService platformToAnchorAuthService(IntegrationAuthConfig integrationAuthConfig) {
    return new JwtService(integrationAuthConfig.getPlatformToAnchorJwtSecret());
  }

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
    return new RestCustomerIntegration(
        sep12Config.getCustomerIntegrationEndPoint(), httpClient, gson);
  }

  @Bean
  RateIntegration rateIntegration(
      AppConfig appConfig,
      Sep38Config sep38Config,
      OkHttpClient httpClient,
      JwtService platformToAnchorAuthService,
      IntegrationAuthConfig integrationAuthConfig,
      Gson gson) {
    return new RestRateIntegration(
        sep38Config.getQuoteIntegrationEndPoint(),
        httpClient,
        platformToAnchorAuthService,
        appConfig.getHostUrl(),
        integrationAuthConfig.getExpirationMilliseconds(),
        gson);
  }

  @Bean
  FeeIntegration feeIntegration(Sep31Config sep31Config, Gson gson) {
    return new RestFeeIntegration(sep31Config.getFeeIntegrationEndPoint(), httpClient(), gson);
  }
}

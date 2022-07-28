package org.stellar.anchor.platform;

import com.google.gson.Gson;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.stellar.anchor.api.callback.CustomerIntegration;
import org.stellar.anchor.api.callback.FeeIntegration;
import org.stellar.anchor.api.callback.RateIntegration;
import org.stellar.anchor.api.callback.UniqueAddressIntegration;
import org.stellar.anchor.auth.AuthHelper;
import org.stellar.anchor.auth.JwtService;
import org.stellar.anchor.config.*;
import org.stellar.anchor.platform.callback.RestCustomerIntegration;
import org.stellar.anchor.platform.callback.RestFeeIntegration;
import org.stellar.anchor.platform.callback.RestRateIntegration;
import org.stellar.anchor.platform.callback.RestUniqueAddressIntegration;

@Configuration
public class IntegrationConfig {
  @Bean
  AuthHelper authHelper(AppConfig appConfig, IntegrationAuthConfig integrationAuthConfig) {
    String authSecret = integrationAuthConfig.getPlatformToAnchorSecret();
    switch (integrationAuthConfig.getAuthType()) {
      case JWT_TOKEN:
        return AuthHelper.forJwtToken(
            new JwtService(authSecret),
            integrationAuthConfig.getExpirationMilliseconds(),
            appConfig.getHostUrl());

      case API_KEY:
        return AuthHelper.forApiKey(authSecret);

      default:
        return AuthHelper.forNone();
    }
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
  UniqueAddressIntegration uniqueAddressIntegration(
      Sep31Config sep31Config, OkHttpClient httpClient, AuthHelper authHelper, Gson gson) {
    return new RestUniqueAddressIntegration(
        sep31Config.getUniqueAddressIntegrationEndPoint(), httpClient, authHelper, gson);
  }

  @Bean
  CustomerIntegration customerIntegration(
      Sep12Config sep12Config, OkHttpClient httpClient, AuthHelper authHelper, Gson gson) {
    return new RestCustomerIntegration(
        sep12Config.getCustomerIntegrationEndPoint(), httpClient, authHelper, gson);
  }

  @Bean
  RateIntegration rateIntegration(
      Sep38Config sep38Config, OkHttpClient httpClient, AuthHelper authHelper, Gson gson) {
    return new RestRateIntegration(
        sep38Config.getQuoteIntegrationEndPoint(), httpClient, authHelper, gson);
  }

  @Bean
  FeeIntegration feeIntegration(
      Sep31Config sep31Config, OkHttpClient httpClient, AuthHelper authHelper, Gson gson) {
    return new RestFeeIntegration(
        sep31Config.getFeeIntegrationEndPoint(), httpClient, authHelper, gson);
  }
}

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
import org.stellar.anchor.config.AppConfig;
import org.stellar.anchor.platform.callback.RestCustomerIntegration;
import org.stellar.anchor.platform.callback.RestFeeIntegration;
import org.stellar.anchor.platform.callback.RestRateIntegration;
import org.stellar.anchor.platform.callback.RestUniqueAddressIntegration;
import org.stellar.anchor.platform.config.CallbackApiConfig;

@Configuration
public class CallbackApiBeans {

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
      AppConfig appConfig,
      CallbackApiConfig callbackApiConfig,
      OkHttpClient httpClient,
      Gson gson) {
    AuthHelper authHelper = buildAuthHelper(appConfig, callbackApiConfig);
    return new RestUniqueAddressIntegration(
        callbackApiConfig.getBaseUrl(), httpClient, authHelper, gson);
  }

  @Bean
  CustomerIntegration customerIntegration(
      AppConfig appConfig,
      CallbackApiConfig callbackApiConfig,
      OkHttpClient httpClient,
      Gson gson) {
    return new RestCustomerIntegration(
        callbackApiConfig.getBaseUrl(),
        httpClient,
        buildAuthHelper(appConfig, callbackApiConfig),
        gson);
  }

  @Bean
  RateIntegration rateIntegration(
      AppConfig appConfig,
      CallbackApiConfig callbackApiConfig,
      OkHttpClient httpClient,
      Gson gson) {
    return new RestRateIntegration(
        callbackApiConfig.getBaseUrl(),
        httpClient,
        buildAuthHelper(appConfig, callbackApiConfig),
        gson);
  }

  @Bean
  FeeIntegration feeIntegration(
      AppConfig appConfig,
      CallbackApiConfig callbackApiConfig,
      OkHttpClient httpClient,
      Gson gson) {
    return new RestFeeIntegration(
        callbackApiConfig.getBaseUrl(),
        httpClient,
        buildAuthHelper(appConfig, callbackApiConfig),
        gson);
  }

  AuthHelper buildAuthHelper(AppConfig appConfig, CallbackApiConfig callbackApiConfig) {
    String authSecret = callbackApiConfig.getAuth().getSecret();
    switch (callbackApiConfig.getAuth().getType()) {
      case JWT_TOKEN:
        return AuthHelper.forJwtToken(
            new JwtService(authSecret),
            Long.parseLong(callbackApiConfig.getAuth().getExpirationMilliseconds()),
            appConfig.getHostUrl());
      case API_KEY:
        return AuthHelper.forApiKey(authSecret);
      default:
        return AuthHelper.forNone();
    }
  }
}

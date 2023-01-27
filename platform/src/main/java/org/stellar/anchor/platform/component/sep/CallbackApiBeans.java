package org.stellar.anchor.platform.component.sep;

import com.google.gson.Gson;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import okhttp3.OkHttpClient;
import okhttp3.OkHttpClient.Builder;
import org.springframework.boot.context.properties.ConfigurationProperties;
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
import org.stellar.anchor.platform.config.PropertySecretConfig;

@Configuration
public class CallbackApiBeans {
  TrustManager[] trustAllCerts =
      new TrustManager[] {
        new X509TrustManager() {
          @Override
          public void checkClientTrusted(
              java.security.cert.X509Certificate[] chain, String authType) {}

          @Override
          public void checkServerTrusted(
              java.security.cert.X509Certificate[] chain, String authType) {}

          @Override
          public java.security.cert.X509Certificate[] getAcceptedIssuers() {
            return new java.security.cert.X509Certificate[] {};
          }
        }
      };

  @Bean
  @ConfigurationProperties(prefix = "callback-api")
  CallbackApiConfig callbackApiConfig(PropertySecretConfig secretConfig) {
    return new CallbackApiConfig(secretConfig);
  }

  @Bean
  OkHttpClient httpClient(CallbackApiConfig callbackApiConfig)
      throws NoSuchAlgorithmException, KeyManagementException {
    Builder builder =
        new Builder()
            .connectTimeout(10, TimeUnit.MINUTES)
            .readTimeout(10, TimeUnit.MINUTES)
            .writeTimeout(10, TimeUnit.MINUTES)
            .callTimeout(10, TimeUnit.MINUTES);

    if (!callbackApiConfig.getCheckCertificate()) {
      SSLContext sslContext = SSLContext.getInstance("SSL");
      sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
      builder
          .sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) trustAllCerts[0])
          .hostnameVerifier((hostname, session) -> true);
    }
    return builder.build();
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
      case JWT:
        return AuthHelper.forJwtToken(
            new JwtService(authSecret),
            Long.parseLong(callbackApiConfig.getAuth().getJwt().getExpirationMilliseconds()),
            appConfig.getHostUrl());
      case API_KEY:
        return AuthHelper.forApiKey(authSecret);
      default:
        return AuthHelper.forNone();
    }
  }
}

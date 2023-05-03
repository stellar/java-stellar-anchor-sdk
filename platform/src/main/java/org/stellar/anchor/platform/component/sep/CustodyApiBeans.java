package org.stellar.anchor.platform.component.sep;

import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import okhttp3.OkHttpClient.Builder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.stellar.anchor.auth.AuthHelper;
import org.stellar.anchor.auth.JwtService;
import org.stellar.anchor.config.AppConfig;
import org.stellar.anchor.platform.apiclient.CustodyApiClient;
import org.stellar.anchor.platform.config.CustodyApiConfig;
import org.stellar.anchor.platform.config.CustodySecretConfig;

@Configuration
@ConditionalOnExpression(value = "'${custody.type}' != 'none'")
public class CustodyApiBeans {

  @Bean
  @ConfigurationProperties(prefix = "custody-server")
  public CustodyApiConfig custodyApiConfig(CustodySecretConfig custodySecretConfig) {
    return new CustodyApiConfig(custodySecretConfig);
  }

  @Bean(name = "custodyHttpClient")
  OkHttpClient custodyHttpClient() {
    return new Builder()
        .connectTimeout(10, TimeUnit.MINUTES)
        .readTimeout(10, TimeUnit.MINUTES)
        .writeTimeout(10, TimeUnit.MINUTES)
        .callTimeout(10, TimeUnit.MINUTES)
        .build();
  }

  @Bean
  CustodyApiClient custodyApiClient(
      @Qualifier("custodyHttpClient") OkHttpClient httpClient,
      AppConfig appConfig,
      CustodyApiConfig custodyApiConfig) {
    return new CustodyApiClient(
        httpClient, buildAuthHelper(appConfig, custodyApiConfig), custodyApiConfig);
  }

  AuthHelper buildAuthHelper(AppConfig appConfig, CustodyApiConfig custodyApiConfig) {
    String secret = custodyApiConfig.getAuth().getSecret();
    switch (custodyApiConfig.getAuth().getType()) {
      case JWT:
        return AuthHelper.forJwtToken(
            new JwtService(secret, null, null),
            Long.parseLong(custodyApiConfig.getAuth().getJwt().getExpirationMilliseconds()),
            appConfig.getHostUrl());
      case API_KEY:
        return AuthHelper.forApiKey(secret);
      default:
        return AuthHelper.forNone();
    }
  }
}

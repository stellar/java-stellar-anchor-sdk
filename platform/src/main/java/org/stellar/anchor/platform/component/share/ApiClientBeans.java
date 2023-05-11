package org.stellar.anchor.platform.component.share;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.stellar.anchor.apiclient.PlatformApiClient;
import org.stellar.anchor.auth.AuthHelper;
import org.stellar.anchor.auth.JwtService;
import org.stellar.anchor.config.AppConfig;
import org.stellar.anchor.platform.config.PlatformApiConfig;

@Configuration
public class ApiClientBeans {
  @Bean
  PlatformApiClient platformApiClient(AppConfig appConfig, PlatformApiConfig platformApiConfig) {
    return new PlatformApiClient(
        buildAuthHelper(appConfig, platformApiConfig), platformApiConfig.getBaseUrl());
  }

  AuthHelper buildAuthHelper(AppConfig appConfig, PlatformApiConfig platformApiConfig) {
    String secret = platformApiConfig.getAuth().getSecret();
    switch (platformApiConfig.getAuth().getType()) {
      case JWT:
        return AuthHelper.forJwtToken(
            new JwtService(secret, null, null),
            Long.parseLong(platformApiConfig.getAuth().getJwt().getExpirationMilliseconds()),
            appConfig.getHostUrl());
      case API_KEY:
        return AuthHelper.forApiKey(secret);
      default:
        return AuthHelper.forNone();
    }
  }
}

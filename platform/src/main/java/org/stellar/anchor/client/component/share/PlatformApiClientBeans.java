package org.stellar.anchor.client.component.share;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.stellar.anchor.apiclient.PlatformApiClient;
import org.stellar.anchor.auth.AuthHelper;
import org.stellar.anchor.client.config.PlatformApiConfig;

@Configuration
public class PlatformApiClientBeans {
  @Bean
  PlatformApiClient platformApiClient(PlatformApiConfig platformApiConfig, AuthHelper authHelper) {
    return new PlatformApiClient(authHelper, platformApiConfig.getBaseUrl());
  }

  @Bean
  AuthHelper authHelper(PlatformApiConfig platformApiConfig) {
    return AuthHelper.from(
        platformApiConfig.getAuth().getType(),
        platformApiConfig.getAuth().getSecret(),
        Long.parseLong(platformApiConfig.getAuth().getJwt().getExpirationMilliseconds()));
  }
}

package org.stellar.anchor.server;

import java.io.IOException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.stellar.anchor.asset.AssetService;
import org.stellar.anchor.asset.ResourceJsonAssetService;
import org.stellar.anchor.config.AppConfig;
import org.stellar.anchor.config.Sep10Config;
import org.stellar.anchor.config.Sep1Config;
import org.stellar.anchor.config.Sep38Config;
import org.stellar.anchor.exception.SepNotFoundException;
import org.stellar.anchor.filter.Sep10TokenFilter;
import org.stellar.anchor.horizon.Horizon;
import org.stellar.anchor.sep1.Sep1Service;
import org.stellar.anchor.sep10.JwtService;
import org.stellar.anchor.sep10.Sep10Service;
import org.stellar.anchor.sep38.Sep38Service;

/** SEP configurations */
@Configuration
public class SepConfig {
  public SepConfig() {}

  /**
   * Register sep-10 token filter.
   *
   * @return Spring Filter Registration Bean
   */
  @Bean
  public FilterRegistrationBean<Sep10TokenFilter> sep10TokenFilter(
      @Autowired Sep10Config sep10Config, @Autowired JwtService jwtService) {
    FilterRegistrationBean<Sep10TokenFilter> registrationBean = new FilterRegistrationBean<>();
    registrationBean.setFilter(new Sep10TokenFilter(sep10Config, jwtService));
    return registrationBean;
  }

  @Bean
  public JwtService jwtService(AppConfig appConfig) {
    return new JwtService(appConfig);
  }

  @Bean
  public Horizon horizon(AppConfig appConfig) {
    return new Horizon(appConfig);
  }

  @Bean
  AssetService assetService(AppConfig appConfig) throws IOException, SepNotFoundException {
    return new ResourceJsonAssetService(appConfig.getAssets());
  }

  @Bean
  Sep1Service sep1Service(Sep1Config sep1Config) {
    return new Sep1Service(sep1Config);
  }

  @Bean
  Sep10Service sep10Service(
      AppConfig appConfig, Sep10Config sep10Config, Horizon horizon, JwtService jwtService) {
    return new Sep10Service(appConfig, sep10Config, horizon, jwtService);
  }

  @Bean
  Sep38Service sep38Service(Sep38Config sep38Config, AssetService assetService) {
    return new Sep38Service(sep38Config, assetService);
  }
}

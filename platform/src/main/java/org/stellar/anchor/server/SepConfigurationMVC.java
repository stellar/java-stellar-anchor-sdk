package org.stellar.anchor.server;

import java.io.IOException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.stellar.anchor.config.AppConfig;
import org.stellar.anchor.config.Sep10Config;
import org.stellar.anchor.config.Sep1Config;
import org.stellar.anchor.config.Sep24Config;
import org.stellar.anchor.exception.SepNotFoundException;
import org.stellar.anchor.filter.Sep10TokenFilter;
import org.stellar.anchor.horizon.Horizon;
import org.stellar.anchor.plugins.asset.ResourceJsonAssetService;
import org.stellar.anchor.sep1.Sep1Service;
import org.stellar.anchor.sep10.JwtService;
import org.stellar.anchor.sep10.Sep10Service;
import org.stellar.anchor.sep24.AssetService;
import org.stellar.anchor.sep24.Sep24Service;
import org.stellar.anchor.server.config.PropertyResourceAppConfig;
import org.stellar.anchor.server.config.PropertyResourceSep10Config;
import org.stellar.anchor.server.config.PropertyResourceSep1Config;
import org.stellar.anchor.server.config.PropertyResourceSep24Config;
import org.stellar.anchor.server.data.JdbcSep24TransactionRepo;
import org.stellar.anchor.server.data.JdbcSep24TransactionStore;

@Configuration
public class SepConfigurationMVC {
  public SepConfigurationMVC() {}

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
    registrationBean.addUrlPatterns("/sep24/transactions/*");
    registrationBean.addUrlPatterns("/sep24/transaction");

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
  JdbcSep24TransactionStore sep24TransactionStore(JdbcSep24TransactionRepo txnRepo) {
    return new JdbcSep24TransactionStore(txnRepo);
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
  Sep24Service sep24Service(
      AppConfig appConfig,
      Sep24Config sep24Config,
      AssetService assetService,
      JwtService jwtService,
      JdbcSep24TransactionStore txnStore) {
    return new Sep24Service(appConfig, sep24Config, assetService, jwtService, txnStore);
  }

  @Bean
  @ConfigurationProperties("app")
  AppConfig appConfig() {
    return new PropertyResourceAppConfig();
  }

  @Bean
  @ConfigurationProperties(prefix = "sep1")
  Sep1Config sep1Config() {
    return new PropertyResourceSep1Config();
  }

  @Bean
  @ConfigurationProperties(prefix = "sep10")
  Sep10Config sep10Config() {
    return new PropertyResourceSep10Config();
  }

  @Bean
  @ConfigurationProperties(prefix = "sep24")
  Sep24Config sep24Config() {
    return new PropertyResourceSep24Config();
  }
}

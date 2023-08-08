package org.stellar.anchor.platform.component.sep;

import java.io.IOException;
import javax.servlet.Filter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.stellar.anchor.api.callback.CustomerIntegration;
import org.stellar.anchor.api.callback.FeeIntegration;
import org.stellar.anchor.api.callback.RateIntegration;
import org.stellar.anchor.api.callback.UniqueAddressIntegration;
import org.stellar.anchor.api.exception.InvalidConfigException;
import org.stellar.anchor.asset.AssetService;
import org.stellar.anchor.auth.JwtService;
import org.stellar.anchor.config.*;
import org.stellar.anchor.event.EventService;
import org.stellar.anchor.filter.Sep10JwtFilter;
import org.stellar.anchor.horizon.Horizon;
import org.stellar.anchor.platform.condition.ConditionalOnAllSepsEnabled;
import org.stellar.anchor.platform.config.*;
import org.stellar.anchor.platform.observer.stellar.PaymentObservingAccountsManager;
import org.stellar.anchor.platform.service.Sep31DepositInfoGeneratorApi;
import org.stellar.anchor.platform.service.Sep31DepositInfoGeneratorSelf;
import org.stellar.anchor.platform.service.SimpleInteractiveUrlConstructor;
import org.stellar.anchor.platform.service.SimpleMoreInfoUrlConstructor;
import org.stellar.anchor.sep1.Sep1Service;
import org.stellar.anchor.sep10.Sep10Service;
import org.stellar.anchor.sep12.Sep12Service;
import org.stellar.anchor.sep24.InteractiveUrlConstructor;
import org.stellar.anchor.sep24.MoreInfoUrlConstructor;
import org.stellar.anchor.sep24.Sep24Service;
import org.stellar.anchor.sep24.Sep24TransactionStore;
import org.stellar.anchor.sep31.Sep31DepositInfoGenerator;
import org.stellar.anchor.sep31.Sep31Service;
import org.stellar.anchor.sep31.Sep31TransactionStore;
import org.stellar.anchor.sep38.Sep38QuoteStore;
import org.stellar.anchor.sep38.Sep38Service;

/** SEP configurations */
@Configuration
public class SepBeans {
  /**********************************
   * SEP configurations
   */
  @Bean
  @ConfigurationProperties(prefix = "sep1")
  Sep1Config sep1Config() {
    return new PropertySep1Config();
  }

  @Bean
  @ConfigurationProperties(prefix = "sep10")
  Sep10Config sep10Config(AppConfig appConfig, SecretConfig secretConfig) {
    return new PropertySep10Config(appConfig, secretConfig);
  }

  @Bean
  @ConfigurationProperties(prefix = "sep12")
  Sep12Config sep12Config(CallbackApiConfig callbackApiConfig) {
    return new PropertySep12Config(callbackApiConfig);
  }

  @Bean
  @ConfigurationProperties(prefix = "sep24")
  PropertySep24Config sep24Config(SecretConfig secretConfig) {
    return new PropertySep24Config(secretConfig);
  }

  @Bean
  @ConfigurationProperties(prefix = "sep31")
  Sep31Config sep31Config() {
    return new PropertySep31Config();
  }

  @Bean
  @ConfigurationProperties(prefix = "sep38")
  Sep38Config sep38Config() {
    return new PropertySep38Config();
  }

  /**
   * Register sep-10 token filter.
   *
   * @return Spring Filter Registration Bean
   */
  @Bean
  public FilterRegistrationBean<Filter> sep10TokenFilter(JwtService jwtService) {
    FilterRegistrationBean<Filter> registrationBean = new FilterRegistrationBean<>();
    registrationBean.setFilter(new Sep10JwtFilter(jwtService));
    registrationBean.addUrlPatterns("/sep12/*");
    registrationBean.addUrlPatterns("/sep24/transaction");
    registrationBean.addUrlPatterns("/sep24/transactions*");
    registrationBean.addUrlPatterns("/sep24/transactions/*");
    registrationBean.addUrlPatterns("/sep31/transactions");
    registrationBean.addUrlPatterns("/sep31/transactions/*");
    registrationBean.addUrlPatterns("/sep38/quote");
    registrationBean.addUrlPatterns("/sep38/quote/*");
    return registrationBean;
  }

  @Bean
  @ConditionalOnAllSepsEnabled(seps = {"sep1"})
  Sep1Service sep1Service(Sep1Config sep1Config) throws IOException, InvalidConfigException {
    return new Sep1Service(sep1Config);
  }

  @Bean
  @ConditionalOnAllSepsEnabled(seps = {"sep10"})
  Sep10Service sep10Service(
      AppConfig appConfig,
      SecretConfig secretConfig,
      Sep10Config sep10Config,
      Horizon horizon,
      JwtService jwtService) {
    return new Sep10Service(appConfig, secretConfig, sep10Config, horizon, jwtService);
  }

  @Bean
  @ConditionalOnAllSepsEnabled(seps = {"sep12"})
  Sep12Service sep12Service(CustomerIntegration customerIntegration, AssetService assetService) {
    return new Sep12Service(customerIntegration, assetService);
  }

  @Bean
  @ConditionalOnAllSepsEnabled(seps = {"sep24"})
  Sep24Service sep24Service(
      AppConfig appConfig,
      Sep24Config sep24Config,
      AssetService assetService,
      JwtService jwtService,
      Sep24TransactionStore sep24TransactionStore,
      EventService eventService,
      InteractiveUrlConstructor interactiveUrlConstructor,
      MoreInfoUrlConstructor moreInfoUrlConstructor) {
    return new Sep24Service(
        appConfig,
        sep24Config,
        assetService,
        jwtService,
        sep24TransactionStore,
        eventService,
        interactiveUrlConstructor,
        moreInfoUrlConstructor);
  }

  @Bean
  InteractiveUrlConstructor interactiveUrlConstructor(
      PropertySep24Config sep24Config,
      CustomerIntegration customerIntegration,
      JwtService jwtService) {
    return new SimpleInteractiveUrlConstructor(sep24Config, customerIntegration, jwtService);
  }

  @Bean
  MoreInfoUrlConstructor moreInfoUrlConstructor(
      PropertySep24Config sep24Config, JwtService jwtService) {
    return new SimpleMoreInfoUrlConstructor(sep24Config.getMoreInfoUrl(), jwtService);
  }

  @Bean
  Sep31DepositInfoGenerator sep31DepositInfoGenerator(
      Sep31Config sep31Config,
      PaymentObservingAccountsManager paymentObservingAccountsManager,
      UniqueAddressIntegration uniqueAddressIntegration) {
    switch (sep31Config.getDepositInfoGeneratorType()) {
      case SELF:
        return new Sep31DepositInfoGeneratorSelf();
      case API:
        return new Sep31DepositInfoGeneratorApi(
            uniqueAddressIntegration, paymentObservingAccountsManager);
      default:
        throw new RuntimeException("Not supported");
    }
  }

  @Bean
  @ConditionalOnAllSepsEnabled(seps = {"sep31"})
  Sep31Service sep31Service(
      AppConfig appConfig,
      Sep31Config sep31Config,
      Sep31TransactionStore sep31TransactionStore,
      Sep31DepositInfoGenerator sep31DepositInfoGenerator,
      Sep38QuoteStore sep38QuoteStore,
      AssetService assetService,
      FeeIntegration feeIntegration,
      CustomerIntegration customerIntegration,
      EventService eventService) {
    return new Sep31Service(
        appConfig,
        sep31Config,
        sep31TransactionStore,
        sep31DepositInfoGenerator,
        sep38QuoteStore,
        assetService,
        feeIntegration,
        customerIntegration,
        eventService);
  }

  @Bean
  @ConditionalOnAllSepsEnabled(seps = {"sep38"})
  Sep38Service sep38Service(
      Sep38Config sep38Config,
      AssetService assetService,
      RateIntegration rateIntegration,
      Sep38QuoteStore sep38QuoteStore,
      EventService eventService) {
    return new Sep38Service(
        sep38Config, assetService, rateIntegration, sep38QuoteStore, eventService);
  }
}

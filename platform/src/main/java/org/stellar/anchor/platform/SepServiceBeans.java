package org.stellar.anchor.platform;

import com.google.gson.Gson;
import java.io.IOException;
import javax.servlet.Filter;
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
import org.stellar.anchor.filter.JwtTokenFilter;
import org.stellar.anchor.horizon.Horizon;
import org.stellar.anchor.platform.data.*;
import org.stellar.anchor.platform.payment.observer.circle.CirclePaymentService;
import org.stellar.anchor.platform.payment.observer.stellar.PaymentObservingAccountsManager;
import org.stellar.anchor.platform.service.PropertyAssetsService;
import org.stellar.anchor.platform.service.Sep31DepositInfoGeneratorApi;
import org.stellar.anchor.platform.service.Sep31DepositInfoGeneratorCircle;
import org.stellar.anchor.platform.service.Sep31DepositInfoGeneratorSelf;
import org.stellar.anchor.sep1.Sep1Service;
import org.stellar.anchor.sep10.Sep10Service;
import org.stellar.anchor.sep12.Sep12Service;
import org.stellar.anchor.sep24.Sep24Service;
import org.stellar.anchor.sep24.Sep24TransactionStore;
import org.stellar.anchor.sep31.Sep31DepositInfoGenerator;
import org.stellar.anchor.sep31.Sep31Service;
import org.stellar.anchor.sep31.Sep31TransactionStore;
import org.stellar.anchor.sep38.Sep38QuoteStore;
import org.stellar.anchor.sep38.Sep38Service;

/** SEP configurations */
@Configuration
public class SepServiceBeans {
  public SepServiceBeans() {}

  /**
   * Used by SEP-10 authentication service.
   *
   * @return the jwt service used by SEP-10.
   */
  @Bean
  public JwtService jwtService(SecretConfig secretConfig) {
    return new JwtService(secretConfig);
  }

  /**
   * Register sep-10 token filter.
   *
   * @return Spring Filter Registration Bean
   */
  @Bean
  public FilterRegistrationBean<Filter> sep10TokenFilter(JwtService jwtService) {
    FilterRegistrationBean<Filter> registrationBean = new FilterRegistrationBean<>();
    registrationBean.setFilter(new JwtTokenFilter(jwtService));
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
  AssetService assetService(AssetsConfig assetsConfig) throws InvalidConfigException {
    return new PropertyAssetsService(assetsConfig);
  }

  @Bean
  public Horizon horizon(AppConfig appConfig) {
    return new Horizon(appConfig);
  }

  @Bean
  Sep1Service sep1Service(Sep1Config sep1Config) throws IOException {
    return new Sep1Service(sep1Config);
  }

  @Bean
  Sep10Service sep10Service(
      AppConfig appConfig,
      SecretConfig secretConfig,
      Sep10Config sep10Config,
      Horizon horizon,
      JwtService jwtService) {
    return new Sep10Service(appConfig, secretConfig, sep10Config, horizon, jwtService);
  }

  @Bean
  Sep12Service sep12Service(CustomerIntegration customerIntegration) {
    return new Sep12Service(customerIntegration);
  }

  @Bean
  Sep24Service sep24Service(
      Gson gson,
      AppConfig appConfig,
      Sep24Config sep24Config,
      AssetService assetService,
      JwtService jwtService,
      Sep24TransactionStore sep24TransactionStore) {
    return new Sep24Service(
        gson, appConfig, sep24Config, assetService, jwtService, sep24TransactionStore);
  }

  @Bean
  Sep24TransactionStore sep24TransactionStore(JdbcSep24TransactionRepo sep24TransactionRepo) {
    return new JdbcSep24TransactionStore(sep24TransactionRepo);
  }

  @Bean
  CirclePaymentService circlePaymentService(
      PaymentObserverConfig paymentObserverConfig, Horizon horizon) {
    return new CirclePaymentService(paymentObserverConfig, horizon);
  }

  @Bean
  Sep31DepositInfoGenerator sep31DepositInfoGenerator(
      Sep31Config sep31Config,
      CirclePaymentService circlePaymentService,
      PaymentObservingAccountsManager paymentObservingAccountsManager,
      UniqueAddressIntegration uniqueAddressIntegration) {
    switch (sep31Config.getDepositInfoGeneratorType()) {
      case SELF:
        return new Sep31DepositInfoGeneratorSelf();

      case CIRCLE:
        return new Sep31DepositInfoGeneratorCircle(circlePaymentService);

      case API:
        return new Sep31DepositInfoGeneratorApi(
            uniqueAddressIntegration, paymentObservingAccountsManager);
      default:
        throw new RuntimeException("Not supported");
    }
  }

  @Bean
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
  JdbcSep31TransactionStore sep31TransactionStore(JdbcSep31TransactionRepo txnRepo) {
    return new JdbcSep31TransactionStore(txnRepo);
  }

  @Bean
  Sep38QuoteStore sep38QuoteStore(JdbcSep38QuoteRepo quoteRepo) {
    return new JdbcSep38QuoteStore(quoteRepo);
  }

  @Bean
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

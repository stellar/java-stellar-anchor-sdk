package org.stellar.anchor.platform;

import com.google.gson.Gson;
import javax.servlet.Filter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.stellar.anchor.api.callback.CustomerIntegration;
import org.stellar.anchor.api.callback.FeeIntegration;
import org.stellar.anchor.api.callback.RateIntegration;
import org.stellar.anchor.api.callback.UniqueAddressIntegration;
import org.stellar.anchor.asset.AssetService;
import org.stellar.anchor.auth.JwtService;
import org.stellar.anchor.config.*;
import org.stellar.anchor.event.EventPublishService;
import org.stellar.anchor.filter.ApiKeyFilter;
import org.stellar.anchor.filter.JwtTokenFilter;
import org.stellar.anchor.filter.NoneFilter;
import org.stellar.anchor.horizon.Horizon;
import org.stellar.anchor.platform.data.*;
import org.stellar.anchor.platform.payment.config.CirclePaymentConfig;
import org.stellar.anchor.platform.payment.observer.circle.CirclePaymentService;
import org.stellar.anchor.platform.payment.observer.stellar.PaymentObservingAccountsManager;
import org.stellar.anchor.platform.service.*;
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
import org.stellar.anchor.util.ResourceReader;

/** SEP configurations */
@Configuration
public class SepConfig {
  public SepConfig() {}

  /**
   * Used by SEP-10 authentication service.
   *
   * @return the jwt service used by SEP-10.
   */
  @Bean
  public JwtService jwtService(AppConfig appConfig) {
    return new JwtService(appConfig);
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

  /**
   * Register anchor-to-platform token filter.
   *
   * @return Spring Filter Registration Bean
   */
  @Bean
  public FilterRegistrationBean<Filter> anchorToPlatformTokenFilter(
      IntegrationAuthConfig integrationAuthConfig) {
    Filter anchorToPlatformFilter;
    String authSecret = integrationAuthConfig.getAnchorToPlatformSecret();
    switch (integrationAuthConfig.getAuthType()) {
      case JWT_TOKEN:
        JwtService jwtService = new JwtService(authSecret);
        anchorToPlatformFilter = new JwtTokenFilter(jwtService);
        break;

      case API_KEY:
        anchorToPlatformFilter = new ApiKeyFilter(authSecret);
        break;

      default:
        anchorToPlatformFilter = new NoneFilter();
        break;
    }

    FilterRegistrationBean<Filter> registrationBean = new FilterRegistrationBean<>();
    registrationBean.setFilter(anchorToPlatformFilter);
    registrationBean.addUrlPatterns("/transactions/*");
    registrationBean.addUrlPatterns("/transactions");
    registrationBean.addUrlPatterns("/exchange/quotes/*");
    registrationBean.addUrlPatterns("/exchange/quotes");
    return registrationBean;
  }

  @Bean
  AssetService assetService(AppConfig appConfig, ResourceReader resourceReader) {
    return new ResourceReaderAssetService(appConfig.getAssets(), resourceReader);
  }

  @Bean
  public Horizon horizon(AppConfig appConfig) {
    return new Horizon(appConfig);
  }

  @Bean
  public ResourceReader resourceReader() {
    return new SpringResourceReader();
  }

  @Bean
  Sep1Service sep1Service(Sep1Config sep1Config, ResourceReader resourceReader) {
    return new Sep1Service(sep1Config, resourceReader);
  }

  @Bean
  Sep10Service sep10Service(
      AppConfig appConfig, Sep10Config sep10Config, Horizon horizon, JwtService jwtService) {
    return new Sep10Service(appConfig, sep10Config, horizon, jwtService);
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
      CirclePaymentConfig circlePaymentConfig, CircleConfig circleConfig, Horizon horizon) {
    return new CirclePaymentService(circlePaymentConfig, circleConfig, horizon);
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
      EventPublishService eventPublishService) {
    return new Sep31Service(
        appConfig,
        sep31Config,
        sep31TransactionStore,
        sep31DepositInfoGenerator,
        sep38QuoteStore,
        assetService,
        feeIntegration,
        customerIntegration,
        eventPublishService);
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
      EventPublishService eventService) {
    return new Sep38Service(
        sep38Config, assetService, rateIntegration, sep38QuoteStore, eventService);
  }
}

package org.stellar.anchor.platform;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.FileCopyUtils;
import org.stellar.anchor.asset.AssetService;
import org.stellar.anchor.asset.ResourceJsonAssetService;
import org.stellar.anchor.config.AppConfig;
import org.stellar.anchor.config.Sep10Config;
import org.stellar.anchor.config.Sep1Config;
import org.stellar.anchor.config.Sep38Config;
import org.stellar.anchor.event.EventService;
import org.stellar.anchor.config.*;
import org.stellar.anchor.exception.SepNotFoundException;
import org.stellar.anchor.filter.Sep10TokenFilter;
import org.stellar.anchor.horizon.Horizon;
import org.stellar.anchor.integration.customer.CustomerIntegration;
import org.stellar.anchor.integration.fee.FeeIntegration;
import org.stellar.anchor.integration.rate.RateIntegration;
import org.stellar.anchor.sep1.ResourceReader;
import org.stellar.anchor.sep1.Sep1Service;
import org.stellar.anchor.sep10.JwtService;
import org.stellar.anchor.sep10.Sep10Service;
import org.stellar.anchor.sep12.Sep12Service;
import org.stellar.anchor.sep31.Sep31Service;
import org.stellar.anchor.sep31.Sep31TransactionStore;
import org.stellar.anchor.sep38.Sep38QuoteStore;
import org.stellar.anchor.sep38.Sep38Service;
import org.stellar.anchor.server.data.JdbcSep31TransactionRepo;
import org.stellar.anchor.server.data.JdbcSep31TransactionStore;
import org.stellar.anchor.server.data.JdbcSep38QuoteRepo;
import org.stellar.anchor.server.data.JdbcSep38QuoteStore;

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
    registrationBean.addUrlPatterns("/sep12/*");
    registrationBean.addUrlPatterns("/sep31/*");
    registrationBean.addUrlPatterns("/sep38/quote");
    registrationBean.addUrlPatterns("/sep38/quote/*");
    return registrationBean;
  }

  @Bean
  public JwtService jwtService(AppConfig appConfig) {
    return new JwtService(appConfig);
  }

  @Bean
  AssetService assetService(AppConfig appConfig) throws IOException, SepNotFoundException {
    return new ResourceJsonAssetService(appConfig.getAssets());
  }

  @Bean
  public Horizon horizon(AppConfig appConfig) {
    return new Horizon(appConfig);
  }

  @Bean
  public ResourceReader resourceReader() {
    return new ResourceReader() {
      ResourceLoader resourceLoader = new DefaultResourceLoader();

      @Override
      public String readResourceAsString(String path) {
        Resource resource = resourceLoader.getResource(path);
        return asString(resource);
      }

      public String asString(Resource resource) {
        try (Reader reader = new InputStreamReader(resource.getInputStream(), UTF_8)) {
          return FileCopyUtils.copyToString(reader);
        } catch (IOException e) {
          throw new UncheckedIOException(e);
        }
      }
    };
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
  Sep31Service sep31Service(
      AppConfig appConfig,
      Sep31Config sep31Config,
      Sep31TransactionStore sep31TransactionStore,
      Sep38QuoteStore sep38QuoteStore,
      AssetService assetService,
      FeeIntegration feeIntegration,
      CustomerIntegration customerIntegration,
      EventService eventService
      ) {
    return new Sep31Service(
        appConfig,
        sep31Config,
        sep31TransactionStore,
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
        sep38Config,
        assetService,
        rateIntegration,
        sep38QuoteStore,
        eventService);
  }
}

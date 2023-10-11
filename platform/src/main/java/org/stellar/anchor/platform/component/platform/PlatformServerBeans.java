package org.stellar.anchor.platform.component.platform;

import java.util.Optional;
import javax.servlet.Filter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.stellar.anchor.api.exception.InvalidConfigException;
import org.stellar.anchor.asset.AssetService;
import org.stellar.anchor.auth.JwtService;
import org.stellar.anchor.config.CustodyConfig;
import org.stellar.anchor.config.Sep24Config;
import org.stellar.anchor.custody.CustodyService;
import org.stellar.anchor.event.EventService;
import org.stellar.anchor.filter.ApiKeyFilter;
import org.stellar.anchor.filter.NoneFilter;
import org.stellar.anchor.filter.PlatformAuthJwtFilter;
import org.stellar.anchor.horizon.Horizon;
import org.stellar.anchor.platform.apiclient.CustodyApiClient;
import org.stellar.anchor.platform.config.PlatformServerConfig;
import org.stellar.anchor.platform.config.PropertyCustodyConfig;
import org.stellar.anchor.platform.data.JdbcTransactionPendingTrustRepo;
import org.stellar.anchor.platform.job.TrustlineCheckJob;
import org.stellar.anchor.platform.rpc.NotifyTrustSetHandler;
import org.stellar.anchor.platform.service.Sep24DepositInfoCustodyGenerator;
import org.stellar.anchor.platform.service.Sep24DepositInfoNoneGenerator;
import org.stellar.anchor.platform.service.Sep24DepositInfoSelfGenerator;
import org.stellar.anchor.platform.service.TransactionService;
import org.stellar.anchor.sep24.Sep24DepositInfoGenerator;
import org.stellar.anchor.sep24.Sep24TransactionStore;
import org.stellar.anchor.sep31.Sep31TransactionStore;
import org.stellar.anchor.sep38.Sep38QuoteStore;
import org.stellar.anchor.sep6.Sep6TransactionStore;

@Configuration
public class PlatformServerBeans {

  /**
   * Register anchor-to-platform token filter.
   *
   * @return Spring Filter Registration Bean
   */
  @Bean
  public FilterRegistrationBean<Filter> platformTokenFilter(PlatformServerConfig config) {
    Filter anchorToPlatformFilter;
    String authSecret = config.getSecretConfig().getPlatformAuthSecret();
    switch (config.getAuth().getType()) {
      case JWT:
        JwtService jwtService = new JwtService(null, null, null, null, authSecret, null);
        anchorToPlatformFilter = new PlatformAuthJwtFilter(jwtService);
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
    return registrationBean;
  }

  @Bean
  Sep24DepositInfoGenerator sep24DepositInfoGenerator(
      Sep24Config sep24Config, Optional<CustodyApiClient> custodyApiClient)
      throws InvalidConfigException {
    switch (sep24Config.getDepositInfoGeneratorType()) {
      case SELF:
        return new Sep24DepositInfoSelfGenerator();
      case CUSTODY:
        return new Sep24DepositInfoCustodyGenerator(
            custodyApiClient.orElseThrow(
                () ->
                    new InvalidConfigException("Integration with custody service is not enabled")));
      case NONE:
        return new Sep24DepositInfoNoneGenerator();
      default:
        throw new RuntimeException("Not supported");
    }
  }

  @Bean
  TransactionService transactionService(
      Sep6TransactionStore txn6Store,
      Sep24TransactionStore txn24Store,
      Sep31TransactionStore txn31Store,
      Sep38QuoteStore quoteStore,
      AssetService assetService,
      EventService eventService,
      Sep24DepositInfoGenerator sep24DepositInfoGenerator,
      CustodyService custodyService,
      CustodyConfig custodyConfig) {
    return new TransactionService(
        txn6Store,
        txn24Store,
        txn31Store,
        quoteStore,
        assetService,
        eventService,
        sep24DepositInfoGenerator,
        custodyService,
        custodyConfig);
  }

  @Bean
  TrustlineCheckJob trustlineCheckJob(
      Horizon horizon,
      JdbcTransactionPendingTrustRepo transactionPendingTrustRepo,
      PropertyCustodyConfig custodyConfig,
      NotifyTrustSetHandler notifyTrustSetHandler) {
    if (custodyConfig.isCustodyIntegrationEnabled()) {
      return new TrustlineCheckJob(
          horizon, transactionPendingTrustRepo, custodyConfig, notifyTrustSetHandler);
    } else {
      return null;
    }
  }
}

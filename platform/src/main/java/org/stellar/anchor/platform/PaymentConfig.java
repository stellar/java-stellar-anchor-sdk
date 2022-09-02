package org.stellar.anchor.platform;

import java.util.List;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import okhttp3.OkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.stellar.anchor.api.exception.ServerErrorException;
import org.stellar.anchor.api.sep.AssetInfo;
import org.stellar.anchor.asset.AssetService;
import org.stellar.anchor.config.AppConfig;
import org.stellar.anchor.config.CirclePaymentObserverConfig;
import org.stellar.anchor.horizon.Horizon;
import org.stellar.anchor.platform.data.PaymentObservingAccountRepo;
import org.stellar.anchor.platform.payment.observer.PaymentListener;
import org.stellar.anchor.platform.payment.observer.circle.CirclePaymentObserverService;
import org.stellar.anchor.platform.payment.observer.stellar.PaymentObservingAccountStore;
import org.stellar.anchor.platform.payment.observer.stellar.PaymentObservingAccountsManager;
import org.stellar.anchor.platform.payment.observer.stellar.StellarPaymentObserver;
import org.stellar.anchor.platform.payment.observer.stellar.StellarPaymentStreamerCursorStore;

@Configuration
public class PaymentConfig {
  @Bean
  @Profile("stellar-observer")
  @SneakyThrows
  public StellarPaymentObserver stellarPaymentObserverService(
      AssetService assetService,
      List<PaymentListener> paymentListeners,
      StellarPaymentStreamerCursorStore stellarPaymentStreamerCursorStore,
      PaymentObservingAccountsManager paymentObservingAccountsManager,
      AppConfig appConfig) {
    // validate assetService
    if (assetService == null || assetService.listAllAssets() == null) {
      throw new ServerErrorException("Asset service cannot be empty.");
    }
    List<AssetInfo> stellarAssets =
        assetService.listAllAssets().stream()
            .filter(asset -> asset.getSchema().equals(AssetInfo.Schema.STELLAR))
            .collect(Collectors.toList());
    if (stellarAssets.size() == 0) {
      throw new ServerErrorException("Asset service should contain at least one Stellar asset.");
    }

    // validate paymentListeners
    if (paymentListeners == null || paymentListeners.size() == 0) {
      throw new ServerErrorException(
          "The stellar payment observer service needs at least one listener.");
    }

    // validate paymentStreamerCursorStore
    if (stellarPaymentStreamerCursorStore == null) {
      throw new ServerErrorException("Payment streamer cursor store cannot be empty.");
    }

    // validate appConfig
    if (appConfig == null) {
      throw new ServerErrorException("App config cannot be empty.");
    }

    StellarPaymentObserver stellarPaymentObserverService =
        StellarPaymentObserver.builder()
            .horizonServer(appConfig.getHorizonUrl())
            .paymentTokenStore(stellarPaymentStreamerCursorStore)
            .observers(paymentListeners)
            .paymentObservingAccountManager(paymentObservingAccountsManager)
            .build();

    // Add distribution wallet to the observing list as type RESIDENTIAL
    for (AssetInfo assetInfo : stellarAssets) {
      if (!paymentObservingAccountsManager.lookupAndUpdate(assetInfo.getDistributionAccount())) {
        paymentObservingAccountsManager.upsert(
            assetInfo.getDistributionAccount(),
            PaymentObservingAccountsManager.AccountType.RESIDENTIAL);
      }
    }

    stellarPaymentObserverService.start();
    return stellarPaymentObserverService;
  }

  @Bean
  public PaymentObservingAccountsManager observingAccounts(
      PaymentObservingAccountStore paymentObservingAccountStore) {
    return new PaymentObservingAccountsManager(paymentObservingAccountStore);
  }

  @Bean
  public PaymentObservingAccountStore observingAccountStore(PaymentObservingAccountRepo repo) {
    return new PaymentObservingAccountStore(repo);
  }

  @Bean
  public CirclePaymentObserverService circlePaymentObserverService(
      OkHttpClient httpClient,
      CirclePaymentObserverConfig circlePaymentObserverConfig,
      Horizon horizon,
      List<PaymentListener> paymentListeners) {
    return new CirclePaymentObserverService(
        httpClient, circlePaymentObserverConfig, horizon, paymentListeners);
  }
}

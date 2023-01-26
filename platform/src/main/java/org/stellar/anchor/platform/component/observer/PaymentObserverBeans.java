package org.stellar.anchor.platform.component.observer;

import java.util.List;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.stellar.anchor.api.exception.ServerErrorException;
import org.stellar.anchor.api.sep.AssetInfo;
import org.stellar.anchor.apiclient.PlatformApiClient;
import org.stellar.anchor.asset.AssetService;
import org.stellar.anchor.config.AppConfig;
import org.stellar.anchor.event.EventService;
import org.stellar.anchor.platform.config.PaymentObserverConfig;
import org.stellar.anchor.platform.observer.PaymentListener;
import org.stellar.anchor.platform.observer.stellar.PaymentObservingAccountsManager;
import org.stellar.anchor.platform.observer.stellar.StellarPaymentObserver;
import org.stellar.anchor.platform.observer.stellar.StellarPaymentStreamerCursorStore;
import org.stellar.anchor.platform.service.PaymentOperationToEventListener;
import org.stellar.anchor.sep31.Sep31TransactionStore;

@Configuration
public class PaymentObserverBeans {
  @Bean
  @SneakyThrows
  public StellarPaymentObserver stellarPaymentObserver(
      AssetService assetService,
      List<PaymentListener> paymentListeners,
      StellarPaymentStreamerCursorStore stellarPaymentStreamerCursorStore,
      PaymentObservingAccountsManager paymentObservingAccountsManager,
      AppConfig appConfig,
      PaymentObserverConfig paymentObserverConfig) {
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
      throw new ServerErrorException("AppConfig cannot be empty.");
    }

    if (paymentObserverConfig == null) {
      throw new ServerErrorException("PaymentObserverConfig cannot be empty.");
    }

    StellarPaymentObserver stellarPaymentObserver =
        new StellarPaymentObserver(
            appConfig.getHorizonUrl(),
            paymentObserverConfig.getStellar(),
            paymentListeners,
            paymentObservingAccountsManager,
            stellarPaymentStreamerCursorStore);

    // Add distribution wallet to the observing list as type RESIDENTIAL
    for (AssetInfo assetInfo : stellarAssets) {
      if (!paymentObservingAccountsManager.lookupAndUpdate(assetInfo.getDistributionAccount())) {
        paymentObservingAccountsManager.upsert(
            assetInfo.getDistributionAccount(),
            PaymentObservingAccountsManager.AccountType.RESIDENTIAL);
      }
    }

    stellarPaymentObserver.start();
    return stellarPaymentObserver;
  }

  @Bean
  public PaymentOperationToEventListener paymentOperationToEventListener(
      Sep31TransactionStore transactionStore,
      EventService eventService,
      PlatformApiClient platformApiClient) {
    return new PaymentOperationToEventListener(transactionStore, eventService, platformApiClient);
  }
}

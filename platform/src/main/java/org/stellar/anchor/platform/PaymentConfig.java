package org.stellar.anchor.platform;

import java.util.List;
import java.util.stream.Collectors;
import okhttp3.OkHttpClient;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.stellar.anchor.api.exception.ServerErrorException;
import org.stellar.anchor.api.sep.AssetInfo;
import org.stellar.anchor.asset.AssetService;
import org.stellar.anchor.config.AppConfig;
import org.stellar.anchor.config.CirclePaymentObserverConfig;
import org.stellar.anchor.event.EventPublishService;
import org.stellar.anchor.horizon.Horizon;
import org.stellar.anchor.platform.data.JdbcSep31TransactionStore;
import org.stellar.anchor.platform.paymentobserver.CirclePaymentObserverService;
import org.stellar.anchor.platform.paymentobserver.PaymentListener;
import org.stellar.anchor.platform.paymentobserver.PaymentStreamerCursorStore;
import org.stellar.anchor.platform.paymentobserver.StellarPaymentObserver;
import org.stellar.anchor.platform.service.PaymentOperationToEventListener;

@Configuration
@AutoConfigureOrder(3)
public class PaymentConfig {
  @Bean
  @ConditionalOnClass(EventPublishService.class)
  public PaymentListener paymentOperationToEventListener(
      JdbcSep31TransactionStore transactionStore, EventPublishService eventService) {
    return new PaymentOperationToEventListener(transactionStore, eventService);
  }

  @Bean
  @ConditionalOnClass(PaymentListener.class)
  public StellarPaymentObserver stellarPaymentObserverService(
      AssetService assetService,
      List<PaymentListener> paymentListeners,
      PaymentStreamerCursorStore paymentStreamerCursorStore,
      AppConfig appConfig)
      throws ServerErrorException {
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
    if (paymentStreamerCursorStore == null) {
      throw new ServerErrorException("Payment streamer cursor store cannot be empty.");
    }

    // validate appConfig
    if (appConfig == null) {
      throw new ServerErrorException("App config cannot be empty.");
    }

    StellarPaymentObserver stellarPaymentObserverService =
        StellarPaymentObserver.builder()
            .horizonServer(appConfig.getHorizonUrl())
            .paymentTokenStore(paymentStreamerCursorStore)
            .observers(paymentListeners)
            .accounts(
                stellarAssets.stream()
                    .map(AssetInfo::getDistributionAccount)
                    .collect(Collectors.toList()))
            .build();

    stellarPaymentObserverService.start();
    return stellarPaymentObserverService;
  }

  @Bean
  @ConditionalOnClass(PaymentListener.class)
  public CirclePaymentObserverService circlePaymentObserverService(
      OkHttpClient httpClient,
      CirclePaymentObserverConfig circlePaymentObserverConfig,
      Horizon horizon,
      List<PaymentListener> paymentListeners) {
    return new CirclePaymentObserverService(
        httpClient, circlePaymentObserverConfig, horizon, paymentListeners);
  }
}

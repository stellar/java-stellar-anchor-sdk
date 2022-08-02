package org.stellar.anchor.platform;

import java.util.List;
import java.util.stream.Collectors;
import okhttp3.OkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.stellar.anchor.api.exception.ServerErrorException;
import org.stellar.anchor.api.sep.AssetInfo;
import org.stellar.anchor.asset.AssetService;
import org.stellar.anchor.config.AppConfig;
import org.stellar.anchor.config.CirclePaymentObserverConfig;
import org.stellar.anchor.horizon.Horizon;
import org.stellar.anchor.platform.payment.observer.PaymentListener;
import org.stellar.anchor.platform.payment.observer.circle.CirclePaymentObserverService;
import org.stellar.anchor.platform.payment.observer.stellar.StellarPaymentObserver;
import org.stellar.anchor.platform.payment.observer.stellar.StellarPaymentStreamerCursorStore;

@Configuration
public class PaymentConfig {
  @Bean
  public StellarPaymentObserver stellarPaymentObserverService(
      AssetService assetService,
      List<PaymentListener> paymentListeners,
      StellarPaymentStreamerCursorStore stellarPaymentStreamerCursorStore,
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
            .observingAccounts(
                stellarAssets.stream()
                    .map(AssetInfo::getDistributionAccount)
                    .collect(Collectors.toList()))
            .build();

    stellarPaymentObserverService.start();
    return stellarPaymentObserverService;
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

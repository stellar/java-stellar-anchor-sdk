package org.stellar.anchor.platform;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.stellar.anchor.asset.AssetInfo;
import org.stellar.anchor.asset.AssetService;
import org.stellar.anchor.config.AppConfig;
import org.stellar.anchor.platform.paymentobserver.PaymentListener;
import org.stellar.anchor.platform.paymentobserver.PaymentStreamerCursorStore;
import org.stellar.anchor.platform.paymentobserver.StellarPaymentObserver;

@Configuration
public class PaymentConfig {
  @Bean
  public StellarPaymentObserver stellarPaymentObserverService(
      AssetService assetService,
      List<PaymentListener> paymentListeners,
      PaymentStreamerCursorStore paymentStreamerCursorStore,
      AppConfig appConfig) {
    StellarPaymentObserver.Builder builder =
        StellarPaymentObserver.builder().horizonServer(appConfig.getHorizonUrl());
    assetService.listAllAssets().stream()
        .filter(asset -> asset.getSchema().equals(AssetInfo.Schema.STELLAR))
        .forEach(
            asset -> {
              builder.addAccount(asset.getDistributionAccount());
            });

    // Assign the payment token store
    builder.paymentTokenStore(paymentStreamerCursorStore);

    if (paymentListeners != null) {
      paymentListeners.forEach(builder::addObserver);
    }

    StellarPaymentObserver stellarPaymentObserverService = builder.build();
    stellarPaymentObserverService.start();

    return stellarPaymentObserverService;
  }
}

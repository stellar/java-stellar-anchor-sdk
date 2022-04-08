package org.stellar.anchor.platform;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.stellar.anchor.asset.AssetInfo;
import org.stellar.anchor.asset.AssetService;
import org.stellar.anchor.config.AppConfig;
import org.stellar.anchor.platform.paymentobserver.PaymentListener;
import org.stellar.anchor.platform.paymentobserver.StellarPaymentObserver;

import java.util.List;

@Configuration
public class PaymentConfig {
  @Bean
  public StellarPaymentObserver stellarPaymentObserverService(
      AssetService assetService,
      List<PaymentListener> paymentListeners,
      AppConfig appConfig) {
    StellarPaymentObserver.Builder builder =
        StellarPaymentObserver.builder().horizonServer(appConfig.getHorizonUrl());
    assetService.listAllAssets().stream()
        .filter(asset -> asset.getSchema().equals(AssetInfo.Schema.STELLAR))
        .forEach(
            asset -> {
              builder.addAccount(asset.getDistributionAccount());
            });

    if (paymentListeners != null) {
      paymentListeners.forEach(builder::addObserver);
    }

    StellarPaymentObserver stellarPaymentObserverService = builder.build();
    stellarPaymentObserverService.start();

    return stellarPaymentObserverService;
  }
}

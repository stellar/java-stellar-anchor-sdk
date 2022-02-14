package org.stellar.anchor.server;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.stellar.anchor.paymentservice.PaymentGateway;
import org.stellar.anchor.paymentservice.circle.CirclePaymentService;
import org.stellar.anchor.paymentservice.circle.config.CirclePaymentConfig;
import org.stellar.anchor.paymentservice.circle.config.StellarxPaymentConfig;
import org.stellar.anchor.server.config.payment.PropertyCirclePaymentConfig;
import org.stellar.anchor.server.config.payment.PropertyStellarxPaymentConfig;

/** Payment gateway configurations. */
@Configuration
public class PaymentConfiguration {
  @Bean
  @ConfigurationProperties("payment-gateway.circle")
  CirclePaymentConfig circlePaymentConfig() {
    return new PropertyCirclePaymentConfig();
  }

  @Bean
  @ConfigurationProperties("payment-gateway.stellarx")
  StellarxPaymentConfig stellarxPaymentConfig() {
    return new PropertyStellarxPaymentConfig();
  }

  @Bean
  PaymentGateway paymentGateway(
      CirclePaymentConfig circlePaymentConfig, StellarxPaymentConfig stellarxPaymentConfig) {
    PaymentGateway.Builder builder = new PaymentGateway.Builder();
    if (circlePaymentConfig.isEnabled()) {
      builder.add(new CirclePaymentService(circlePaymentConfig));
    }

    //        TODO: Te be added when StellarxPaymentService is implemented.
    //        if (stellarxPaymentConfig.isEnabled()) {
    //            builder.add(new StellarsPaymentService(stellarxPaymentConfig));
    //        }

    return builder.build();
  }
}

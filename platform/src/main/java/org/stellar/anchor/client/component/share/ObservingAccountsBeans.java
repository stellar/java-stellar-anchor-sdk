package org.stellar.anchor.client.component.share;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.stellar.anchor.client.observer.stellar.PaymentObservingAccountStore;
import org.stellar.anchor.client.observer.stellar.PaymentObservingAccountsManager;

@Configuration
public class ObservingAccountsBeans {
  @Bean
  public PaymentObservingAccountsManager paymentObservingAccountsManager(
      PaymentObservingAccountStore paymentObservingAccountStore) {
    return new PaymentObservingAccountsManager(paymentObservingAccountStore);
  }
}

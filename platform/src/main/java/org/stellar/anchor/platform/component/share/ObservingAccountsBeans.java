package org.stellar.anchor.platform.component.share;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.stellar.anchor.platform.observer.stellar.PaymentObservingAccountStore;
import org.stellar.anchor.platform.observer.stellar.PaymentObservingAccountsManager;

@Configuration
public class ObservingAccountsBeans {
  @Bean
  public PaymentObservingAccountsManager paymentObservingAccountsManager(
      PaymentObservingAccountStore paymentObservingAccountStore) {
    return new PaymentObservingAccountsManager(paymentObservingAccountStore);
  }
}

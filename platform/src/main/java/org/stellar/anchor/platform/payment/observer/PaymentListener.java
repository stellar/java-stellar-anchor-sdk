package org.stellar.anchor.platform.payment.observer;

import org.stellar.anchor.api.exception.EventPublishException;
import org.stellar.anchor.platform.payment.observer.circle.ObservedPayment;

public interface PaymentListener {
  void onReceived(ObservedPayment payment) throws EventPublishException;

  void onSent(ObservedPayment payment);
}

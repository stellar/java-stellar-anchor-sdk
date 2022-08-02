package org.stellar.anchor.platform.payment.observer;

import org.stellar.anchor.platform.payment.observer.circle.ObservedPayment;

public interface PaymentListener {
  void onReceived(ObservedPayment payment);

  void onSent(ObservedPayment payment);
}

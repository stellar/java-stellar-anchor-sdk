package org.stellar.anchor.platform.observer;

import org.stellar.anchor.api.exception.EventPublishException;

public interface PaymentListener {
  void onReceived(ObservedPayment payment) throws EventPublishException;

  void onSent(ObservedPayment payment);
}

package org.stellar.anchor.platform.paymentobserver;

public interface PaymentListener {
  void onReceived(ObservedPayment payment);

  void onSent(ObservedPayment payment);
}

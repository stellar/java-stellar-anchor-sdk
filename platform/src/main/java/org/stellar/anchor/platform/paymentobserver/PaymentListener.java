package org.stellar.anchor.platform.paymentobserver;

import org.stellar.sdk.responses.operations.PaymentOperationResponse;

public interface PaymentListener {
  void onReceived(PaymentOperationResponse payment);

  void onSent(PaymentOperationResponse payment);
}

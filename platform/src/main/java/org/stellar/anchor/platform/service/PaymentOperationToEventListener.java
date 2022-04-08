package org.stellar.anchor.platform.service;

import org.springframework.stereotype.Component;
import org.stellar.anchor.platform.paymentobserver.PaymentListener;
import org.stellar.sdk.responses.operations.PaymentOperationResponse;
import shadow.com.google.gson.Gson;

@Component
public class PaymentOperationToEventListener implements PaymentListener {
  @Override
  public void onReceived(PaymentOperationResponse payment) {
    System.out.println("Received:" + new Gson().toJson(payment));
  }

  @Override
  public void onSent(PaymentOperationResponse payment) {
    System.out.println("Sent:" + new Gson().toJson(payment));
  }
}

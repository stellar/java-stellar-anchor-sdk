package org.stellar.anchor.paymentobserver;

import java.util.Map;
import org.stellar.anchor.config.CirclePaymentObserverConfig;

public class CirclePaymentObserverService {
  private final CirclePaymentObserverConfig circlePaymentObserverConfig;

  public CirclePaymentObserverService(CirclePaymentObserverConfig circlePaymentObserverConfig) {
    this.circlePaymentObserverConfig = circlePaymentObserverConfig;
  }

  public void handleCircleNotification(Map<String, Object> requestBody) {
    System.out.println(requestBody);
  }
}

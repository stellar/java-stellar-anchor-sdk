package org.stellar.anchor.platform.observer;

import java.io.IOException;
import org.stellar.anchor.api.exception.AnchorException;

public interface PaymentListener {
  void onReceived(ObservedPayment payment) throws AnchorException, IOException;

  void onSent(ObservedPayment payment) throws AnchorException, IOException;
}

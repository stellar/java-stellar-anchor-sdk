package org.stellar.anchor.reference.event.processor;

import org.stellar.anchor.api.event.AnchorEvent;
import org.stellar.anchor.util.Log;

public class Sep24EventProcessor implements SepAnchorEventProcessor {
  @Override
  public void onQuoteCreatedEvent(AnchorEvent event) {
    Log.warnF("Unexpected quote created event: {}", event);
  }

  @Override
  public void onTransactionCreated(AnchorEvent event) {
    Log.infoF("Received transaction created event: {}", event);
  }

  @Override
  public void onTransactionError(AnchorEvent event) {
    Log.infoF("Received transaction error event: {}", event);
  }

  @Override
  public void onTransactionStatusChanged(AnchorEvent event) {
    Log.infoF("Received transaction status changed event: {}", event);
  }

  @Override
  public void onCustomerUpdated(AnchorEvent event) {
    Log.infoF("Received KYC updated event: {}", event);
  }
}

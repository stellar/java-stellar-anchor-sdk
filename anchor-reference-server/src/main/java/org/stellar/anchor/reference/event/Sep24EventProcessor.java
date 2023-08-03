package org.stellar.anchor.reference.event;

import org.stellar.anchor.api.event.AnchorEvent;
import org.stellar.anchor.api.platform.PlatformTransactionData;
import org.stellar.anchor.util.Log;

public class Sep24EventProcessor implements IAnchorEventProcessor {
  @Override
  public void onQuoteCreatedEvent(AnchorEvent event) {
    Log.warnF("Unexpected quote created event: {}", event);
  }

  @Override
  public void onTransactionCreated(AnchorEvent event) {
    PlatformTransactionData.Kind kind = event.getTransaction().getKind();
    Log.infoF("Received transaction created event: {}", event);
    switch (kind) {
      case DEPOSIT:
        Log.infoF("Received deposit transaction created event: {}", event);
        break;
      case WITHDRAWAL:
        handleWithdrawalTransactionCreatedEvent(event);
        break;
      default:
        Log.warnF("Unexpected transaction kind: {}");
        break;
    }
  }

  @Override
  public void onTransactionError(AnchorEvent event) {
    // TODO: Implement
  }

  private void handleWithdrawalTransactionCreatedEvent(AnchorEvent event) {
    // TODO: Implement
  }

  @Override
  public void onTransactionStatusChanged(AnchorEvent event) {
    // TODO: Implement
  }

  @Override
  public void onKycUpdatedEvent(AnchorEvent event) {
    Log.infoF("Received KYC updated event: {}", event);
  }
}

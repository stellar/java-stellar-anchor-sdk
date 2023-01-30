package org.stellar.anchor.reference.event;

import java.io.IOException;
import java.util.List;
import org.springframework.stereotype.Component;
import org.stellar.anchor.api.event.AnchorEvent;
import org.stellar.anchor.api.exception.AnchorException;
import org.stellar.anchor.api.platform.PatchTransactionRequest;
import org.stellar.anchor.api.platform.PatchTransactionsRequest;
import org.stellar.anchor.api.platform.PlatformTransactionData;
import org.stellar.anchor.api.sep.SepTransactionStatus;
import org.stellar.anchor.api.shared.Amount;
import org.stellar.anchor.apiclient.PlatformApiClient;
import org.stellar.anchor.auth.AuthHelper;
import org.stellar.anchor.reference.config.AppSettings;
import org.stellar.anchor.util.Log;

@Component
public class AnchorEventProcessor {
  private final PlatformApiClient platformClient;

  AnchorEventProcessor(AppSettings appSettings, AuthHelper authHelper) {
    this.platformClient = new PlatformApiClient(authHelper, appSettings.getPlatformApiEndpoint());
  }

  public void handleQuoteEvent(AnchorEvent event) {
    Log.debugF("Received quote event: {}", event);
    if (!"quote_created".equals(event.getType())) {
      Log.debugF("error: anchor_platform_event - invalid message type '{}'", event.getType());
    }
  }

  public void handleEvent(AnchorEvent event) {
    Log.debugF("Received transaction event: {}", event);
    switch (event.getType()) {
      case TRANSACTION_CREATED:
        break;
      case TRANSACTION_ERROR:
        break;
      case TRANSACTION_STATUS_CHANGED:
        handleTransactionStatusChangedEvent(event);
        break;
      case QUOTE_CREATED:
        handleQuoteEvent(event);
      default:
        Log.debugF("error: anchor_platform_event - invalid message type '{}'", event.getType());
    }
  }

  public void handleTransactionStatusChangedEvent(AnchorEvent event) {
    // NOTE: this code skips processing the received payment and just marks the
    // transaction as complete.
    if (event.getTransaction().getStatus().equals(SepTransactionStatus.COMPLETED)){
      // skip if the transaction is already in COMPLETED state
      return;
    }
    Log.debugF("Updating transaction: {} on Anchor Platform to 'complete'", event.getId());
    PatchTransactionsRequest txnRequest =
        PatchTransactionsRequest.builder()
            .records(
                List.of(
                    PatchTransactionRequest.builder()
                        .transaction(
                            PlatformTransactionData.builder()
                                .id(event.getTransaction().getId())
                                .status(SepTransactionStatus.COMPLETED)
                                .amountOut(
                                    new Amount(
                                        event.getTransaction().getAmountOut().getAmount(),
                                        event.getTransaction().getAmountOut().getAsset()))
                                .build())
                        .build()))
            .build();

    try {
      platformClient.patchTransaction(txnRequest);
    } catch (IOException | AnchorException ex) {
      Log.errorEx(ex);
    }
  }
}

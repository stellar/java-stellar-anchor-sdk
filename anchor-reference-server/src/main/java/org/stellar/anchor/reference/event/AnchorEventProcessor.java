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
    Log.debugF("Received quote event: {}", event.getType());
  }

  public void handleEvent(AnchorEvent event) {
    Log.debugF("Received transaction event: {}", event.getType());
    switch (event.getType()) {
      case TRANSACTION_CREATED:
        if (event.getSep().equals("24")) {
          if (event.getTransaction().getKind().toString().equals("WITHDRAWAL")) {
            handleSep24WithdrawalTransactionCreatedEvent(event);
          } else if (event.getTransaction().getKind().toString().equals("DEPOSIT")) {
            // TODO
            Log.debug("Received deposit created event");
          }
        }
        break;
      case TRANSACTION_ERROR:
        break;
      case TRANSACTION_STATUS_CHANGED:
        handleTransactionStatusChangedEvent(event);
        break;
      case QUOTE_CREATED:
        handleQuoteEvent(event);
        break;
      default:
        Log.debugF("error: anchor_platform_event - invalid message type '{}'", event.getType());
    }
  }

  public void handleSep24WithdrawalTransactionCreatedEvent(AnchorEvent event) {
    SepTransactionStatus eventStatus = event.getTransaction().getStatus();
    SepTransactionStatus newStatus = null;
    switch (eventStatus) {
      case INCOMPLETE:
        // The business server should get KYC and other info from the customer via the interactive
        // flow
        // we will skip that in this implementation and just update the transaction's status  in
        // Anchor Platform
        // to PENDING_USR_TRANSFER_START
        newStatus = SepTransactionStatus.PENDING_USR_TRANSFER_START;
        break;
      case PENDING_USR_TRANSFER_START:
        // skip processing if the transaction is in PENDING_USR_TRANSFER_START
        return;
      case PENDING_ANCHOR:
        // The business server should handle the withdrawal of user funds. we will skip that in this
        // implementation and just update the transaction's status  in Anchor Platform
        // to COMPLETED
        newStatus = SepTransactionStatus.COMPLETED;
        break;
      case COMPLETED:
        // skip processing if the transaction is already in COMPLETED state
        return;
      default:
        Log.debugF("event processing for transaction status '{}' not implemented", eventStatus);
        return;
    }

    Log.debugF("Updating transaction: {} on Anchor Platform to '{}'", event.getId(), newStatus);

    PatchTransactionsRequest txnRequest =
        PatchTransactionsRequest.builder()
            .records(
                List.of(
                    PatchTransactionRequest.builder()
                        .transaction(
                            PlatformTransactionData.builder()
                                .id(event.getTransaction().getId())
                                .status(newStatus)
                                .build())
                        .build()))
            .build();

    try {
      platformClient.patchTransaction(txnRequest);
    } catch (IOException | AnchorException ex) {
      Log.errorEx(ex);
    }
  }

  public void handleTransactionStatusChangedEvent(AnchorEvent event) {
    // NOTE: this code skips processing the received payment and just marks the
    // transaction as complete.
    if (event.getTransaction().getStatus().equals(SepTransactionStatus.COMPLETED)) {
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

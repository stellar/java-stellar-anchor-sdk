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
import org.stellar.anchor.api.shared.StellarPayment;
import org.stellar.anchor.api.shared.StellarTransaction;
import org.stellar.anchor.apiclient.PlatformApiClient;
import org.stellar.anchor.auth.AuthHelper;
import org.stellar.anchor.reference.config.AppSettings;
import org.stellar.anchor.util.Log;
import org.stellar.sdk.*;
import org.stellar.sdk.responses.AccountResponse;
import org.stellar.sdk.responses.SubmitTransactionResponse;
import org.stellar.sdk.responses.operations.OperationResponse;
import org.stellar.sdk.responses.operations.PaymentOperationResponse;

@Component
public class AnchorEventProcessor {
  private final PlatformApiClient platformClient;
  private final AppSettings appSettings;
  private final Server server;

  AnchorEventProcessor(AppSettings appSettings, AuthHelper authHelper) {
    this.appSettings = appSettings;
    this.platformClient = new PlatformApiClient(authHelper, appSettings.getPlatformApiEndpoint());
    this.server = new Server("https://horizon-testnet.stellar.org");
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
        } else if (event.getSep().equals("6")) {
          if (event.getTransaction().getKind().toString().equals("DEPOSIT")) {
            handleSep6DepositTransactionCreatedEvent(event);
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
      case KYC_UPDATED:
        if (event.getSep().equals("6")) {
          handleSep6KycUpdatedEvent(event);
        }
      default:
        Log.debugF("error: anchor_platform_event - invalid message type '{}'", event.getType());
    }
  }

  private void handleSep6KycUpdatedEvent(AnchorEvent event) {
    SepTransactionStatus eventStatus = event.getTransaction().getStatus();
    SepTransactionStatus newStatus = null;

    switch (eventStatus) {
      case PENDING_CUSTOMER_INFO_UPDATE:
        newStatus = SepTransactionStatus.PENDING_USR_TRANSFER_START;
        break;
      default:
        Log.debugF("event processing for transaction status '{}' not implemented", eventStatus);
        return;
    }

    PatchTransactionsRequest txnRequest =
        PatchTransactionsRequest.builder()
            .records(
                List.of(
                    PatchTransactionRequest.builder()
                        .transaction(
                            PlatformTransactionData.builder()
                                .id(event.getTransaction().getId())
                                .how("Make a payment to Bank: STP Account: 646180111803859359")
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

  private void handleSep6DepositTransactionCreatedEvent(AnchorEvent event) {
    SepTransactionStatus eventStatus = event.getTransaction().getStatus();
    SepTransactionStatus newStatus = null;
    String txHash = null;
    StellarPayment stellarPayment = null;

    Log.info("handleSep6DepositTransactionCreatedEvent, eventStatus: " + eventStatus.toString());

    switch (eventStatus) {
      case INCOMPLETE:
        // TODO: maybe this should be moved out of transaction created
        // Assume that the funds have been received by the anchor
        try {
          KeyPair keyPair = KeyPair.fromSecretSeed(appSettings.getSecret());
          AccountResponse account = server.accounts().account(keyPair.getAccountId());
          String[] assetCodeParts = event.getTransaction().getAmountOut().getAsset().split(":");
          String assetCode;
          if (assetCodeParts.length == 3) {
            assetCode = assetCodeParts[1] + ":" + assetCodeParts[2];
          } else if (assetCodeParts.length == 2) {
            assetCode = assetCodeParts[1];
          } else {
            throw new RuntimeException("invalid asset code");
          }

          Asset asset = Asset.create(assetCode);
          String amount = event.getTransaction().getAmountOut().getAmount();
          String destination = event.getTransaction().getDestinationAccount();

          Transaction transaction =
              new TransactionBuilder(account, Network.TESTNET)
                  .setBaseFee(100)
                  .setTimeout(60)
                  .addOperation(new PaymentOperation.Builder(destination, asset, amount).build())
                  .build();
          transaction.sign(KeyPair.fromSecretSeed(appSettings.getSecret()));
          SubmitTransactionResponse response = server.submitTransaction(transaction);

          if (!response.isSuccess()) {
            throw new RuntimeException(
                "failed to send transaction with error: "
                    + response.getExtras().getResultCodes().getTransactionResultCode());
          }

          newStatus = SepTransactionStatus.COMPLETED;
          txHash = response.getHash();

          String operationId =
              server.operations().forTransaction(txHash).execute().getRecords().stream()
                  .filter(operation -> operation instanceof PaymentOperationResponse)
                  .findFirst()
                  .map(OperationResponse::getId)
                  .map(Object::toString)
                  .orElse(null);

          stellarPayment =
              new StellarPayment(
                  operationId,
                  new Amount(amount, assetCode),
                  StellarPayment.Type.PAYMENT,
                  keyPair.getAccountId(),
                  destination);
        } catch (Exception e) {
          throw new RuntimeException("failed to submit transaction", e);
        }

        break;
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
                                .stellarTransactions(
                                    List.of(
                                        StellarTransaction.builder()
                                            .id(txHash)
                                            .payments(List.of(stellarPayment))
                                            .build()))
                                .build())
                        .build()))
            .build();

    try {
      platformClient.patchTransaction(txnRequest);
    } catch (IOException | AnchorException ex) {
      Log.errorEx(ex);
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

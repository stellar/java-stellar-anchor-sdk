package org.stellar.anchor.platform.service;

import static org.stellar.anchor.util.MathHelper.decimal;
import static org.stellar.anchor.util.MathHelper.formatAmount;

import io.micrometer.core.instrument.Metrics;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.apache.commons.codec.DecoderException;
import org.stellar.anchor.api.event.AnchorEvent;
import org.stellar.anchor.api.exception.AnchorException;
import org.stellar.anchor.api.exception.EventPublishException;
import org.stellar.anchor.api.platform.PatchTransactionRequest;
import org.stellar.anchor.api.platform.PatchTransactionsRequest;
import org.stellar.anchor.api.platform.PlatformTransactionData;
import org.stellar.anchor.api.sep.SepTransactionStatus;
import org.stellar.anchor.api.shared.Amount;
import org.stellar.anchor.api.shared.StellarPayment;
import org.stellar.anchor.api.shared.StellarTransaction;
import org.stellar.anchor.apiclient.PlatformApiClient;
import org.stellar.anchor.event.EventService;
import org.stellar.anchor.platform.observer.ObservedPayment;
import org.stellar.anchor.platform.observer.PaymentListener;
import org.stellar.anchor.sep31.Sep31Transaction;
import org.stellar.anchor.sep31.Sep31TransactionStore;
import org.stellar.anchor.util.GsonUtils;
import org.stellar.anchor.util.Log;
import org.stellar.anchor.util.MemoHelper;
import org.stellar.sdk.xdr.MemoType;

public class PaymentOperationToEventListener implements PaymentListener {
  final Sep31TransactionStore transactionStore;
  final EventService eventService;
  private PlatformApiClient platformApiClient;

  public PaymentOperationToEventListener(
      Sep31TransactionStore transactionStore,
      EventService eventService,
      PlatformApiClient platformApiClient) {
    this.transactionStore = transactionStore;
    this.eventService = eventService;
    this.platformApiClient = platformApiClient;
  }

  @Override
  public void onReceived(ObservedPayment payment) throws AnchorException, IOException {
    // Check if payment is connected to a transaction
    if (Objects.toString(payment.getTransactionHash(), "").isEmpty()
        || Objects.toString(payment.getTransactionMemo(), "").isEmpty()) {
      return;
    }

    // Check if the payment contains the expected asset type
    if (!List.of("credit_alphanum4", "credit_alphanum12").contains(payment.getAssetType())) {
      // Asset type does not match
      Log.infoF("{} is not an issued asset.", payment.getAssetType());
      return;
    }

    // Parse memo
    String memo = payment.getTransactionMemo();
    String memoType = payment.getTransactionMemoType();
    if (memoType.equals(MemoHelper.memoTypeAsString(MemoType.MEMO_HASH))) {
      try {
        memo = MemoHelper.convertHexToBase64(payment.getTransactionMemo());
      } catch (DecoderException ex) {
        Log.warnF(
            "The memo type is \"hash\" but the memo string {} could not be parsed as such.", memo);
        Log.warnEx(ex);
      }
    }

    // Find a transaction matching the memo
    Sep31Transaction txn;
    try {
      txn = transactionStore.findByStellarMemo(memo);
      if (txn == null) {
        Log.infoF("Not expecting any transaction with the memo {}.", payment.getTransactionMemo());
        return;
      }
    } catch (AnchorException e) {
      Log.errorF(
          "Error finding transaction that matches the memo {}.", payment.getTransactionMemo());
      e.printStackTrace();
      return;
    }

    // Compare asset code
    String paymentAssetName = "stellar:" + payment.getAssetName();
    if (!txn.getAmountInAsset().equals(paymentAssetName)) {
      Log.warnF(
          "Payment asset {} does not match the expected asset {}.",
          payment.getAssetCode(),
          txn.getAmountInAsset());
      return;
    }

    // parse payment creation time
    Instant paymentTime = parsePaymentTime(payment.getCreatedAt());

    // Build Stellar Transaction object
    StellarTransaction stellarTransaction =
        StellarTransaction.builder()
            .id(payment.getTransactionHash())
            .memo(txn.getStellarMemo())
            .memoType(txn.getStellarMemoType())
            .createdAt(paymentTime)
            .envelope(payment.getTransactionEnvelope())
            .payments(
                List.of(
                    StellarPayment.builder()
                        .id(payment.getId())
                        .paymentType(
                            payment.getType() == ObservedPayment.Type.PAYMENT
                                ? StellarPayment.Type.PAYMENT
                                : StellarPayment.Type.PATH_PAYMENT)
                        .sourceAccount(payment.getFrom())
                        .destinationAccount(payment.getTo())
                        .amount(new Amount(payment.getAmount(), payment.getAssetName()))
                        .build()))
            .build();

    // TODO: this should be taken care of by the RPC actions.
    SepTransactionStatus newStatus = SepTransactionStatus.PENDING_RECEIVER;

    // Check if the payment contains the expected amount (or greater)
    BigDecimal expectedAmount = decimal(txn.getAmountIn());
    BigDecimal gotAmount = decimal(payment.getAmount());
    String message = "Incoming payment for SEP-31 transaction";
    if (gotAmount.compareTo(expectedAmount) >= 0) {
      Log.info(message);
      txn.setTransferReceivedAt(paymentTime);
    } else {
      message =
          String.format(
              "The incoming payment amount was insufficient! Expected: \"%s\", Received: \"%s\"",
              formatAmount(expectedAmount), formatAmount(gotAmount));
      Log.warn(message);
    }

    // Patch transaction
    PatchTransactionsRequest patchTransactionsRequest =
        PatchTransactionsRequest.builder()
            .records(
                Collections.singletonList(
                    PatchTransactionRequest.builder()
                        .transaction(
                            PlatformTransactionData.builder()
                                .updatedAt(paymentTime)
                                .status(newStatus)
                                .stellarTransactions(
                                    StellarTransaction.addOrUpdateTransactions(
                                        txn.getStellarTransactions(), stellarTransaction))
                                .stellarTransactionId(payment.getTransactionHash())
                                .build())
                        .build()))
            .build();
    platformApiClient.patchTransaction(patchTransactionsRequest);

    // Update metrics
    Metrics.counter(AnchorMetrics.SEP31_TRANSACTION.toString(), "status", newStatus.toString())
        .increment();
    Metrics.counter(AnchorMetrics.PAYMENT_RECEIVED.toString(), "asset", payment.getAssetName())
        .increment(Double.parseDouble(payment.getAmount()));
  }

  @Override
  public void onSent(ObservedPayment payment) {
    Log.debug("NOOP PaymentOperationToEventListener#onSent was called.");
  }

  private void sendToQueue(AnchorEvent event) throws EventPublishException {
    eventService.publish(event);
    Log.infoF("Sent to event queue {}", GsonUtils.getInstance().toJson(event));
  }

  Instant parsePaymentTime(String paymentTimeStr) {
    try {
      return DateTimeFormatter.ISO_INSTANT.parse(paymentTimeStr, Instant::from);
    } catch (DateTimeParseException | NullPointerException ex) {
      Log.errorF("Error parsing paymentTimeStr {}.", paymentTimeStr);
      ex.printStackTrace();
      return null;
    }
  }
}

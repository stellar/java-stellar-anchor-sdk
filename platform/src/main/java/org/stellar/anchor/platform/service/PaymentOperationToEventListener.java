package org.stellar.anchor.platform.service;

import static org.stellar.anchor.util.MathHelper.*;

import io.micrometer.core.instrument.Metrics;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.apache.commons.codec.DecoderException;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.stellar.anchor.api.exception.AnchorException;
import org.stellar.anchor.api.exception.EventPublishException;
import org.stellar.anchor.api.exception.SepException;
import org.stellar.anchor.api.shared.Amount;
import org.stellar.anchor.api.shared.StellarPayment;
import org.stellar.anchor.api.shared.StellarTransaction;
import org.stellar.anchor.event.EventPublishService;
import org.stellar.anchor.event.models.*;
import org.stellar.anchor.platform.payment.observer.PaymentListener;
import org.stellar.anchor.platform.payment.observer.circle.ObservedPayment;
import org.stellar.anchor.sep31.Sep31Transaction;
import org.stellar.anchor.sep31.Sep31TransactionStore;
import org.stellar.anchor.util.GsonUtils;
import org.stellar.anchor.util.Log;
import org.stellar.anchor.util.MemoHelper;
import org.stellar.sdk.xdr.MemoType;

@Component
@Profile("stellar-observer")
public class PaymentOperationToEventListener implements PaymentListener {
  final Sep31TransactionStore transactionStore;
  final EventPublishService eventService;

  PaymentOperationToEventListener(
      Sep31TransactionStore transactionStore, EventPublishService eventService) {
    this.transactionStore = transactionStore;
    this.eventService = eventService;
  }

  @Override
  public void onReceived(ObservedPayment payment) throws EventPublishException {
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

    // Update statuses
    TransactionEvent.Status oldStatus = TransactionEvent.Status.from(txn.getStatus());
    TransactionEvent.Status newStatus = TransactionEvent.Status.PENDING_RECEIVER;

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

    // Update the database transaction fields
    txn.setUpdatedAt(paymentTime);
    txn.setStatus(newStatus.toString());
    txn.setStellarTransactionId(payment.getTransactionHash());
    List<StellarTransaction> stellarTransactions =
        StellarTransaction.addOrUpdateTransactions(
            txn.getStellarTransactions(), stellarTransaction);
    txn.setStellarTransactions(stellarTransactions);
    // Save
    try {
      transactionStore.save(txn);
    } catch (SepException ex) {
      Log.errorEx("Error saving Sep31Transaction upon received event", ex);
    }

    // send to the event queue
    TransactionEvent.StatusChange statusChange =
        new TransactionEvent.StatusChange(oldStatus, newStatus);
    TransactionEvent event =
        receivedPaymentToEvent(txn, payment, statusChange, message, stellarTransaction);
    sendToQueue(event);

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

  private void sendToQueue(TransactionEvent event) throws EventPublishException {
    eventService.publish(event);
    Log.infoF("Sent to event queue {}", GsonUtils.getInstance().toJson(event));
  }

  TransactionEvent receivedPaymentToEvent(
      Sep31Transaction txn,
      ObservedPayment payment,
      TransactionEvent.StatusChange statusChange,
      String message,
      StellarTransaction newStellarTransaction) {
    TransactionEvent event =
        TransactionEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .type(TransactionEvent.Type.TRANSACTION_STATUS_CHANGED)
            .id(txn.getId())
            .sep(TransactionEvent.Sep.SEP_31)
            .kind(TransactionEvent.Kind.RECEIVE)
            .status(statusChange.getTo())
            .statusChange(statusChange)
            .amountExpected(new Amount(txn.getAmountExpected(), txn.getAmountInAsset()))
            .amountIn(new Amount(payment.getAmount(), txn.getAmountInAsset()))
            .amountOut(new Amount(txn.getAmountOut(), txn.getAmountOutAsset()))
            // TODO: fix PATCH transaction fails if getAmountOut is null?
            .amountFee(new Amount(txn.getAmountFee(), txn.getAmountFeeAsset()))
            .quoteId(txn.getQuoteId())
            .startedAt(txn.getStartedAt())
            .updatedAt(txn.getUpdatedAt())
            .completedAt(null)
            .transferReceivedAt(txn.getTransferReceivedAt())
            .message(message)
            .refunds(null)
            .stellarTransactions(List.of(newStellarTransaction))
            .externalTransactionId(payment.getExternalTransactionId())
            .custodialTransactionId(null)
            .sourceAccount(payment.getFrom())
            .destinationAccount(payment.getTo())
            .customers(txn.getCustomers())
            .creator(txn.getCreator())
            .build();
    return event;
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

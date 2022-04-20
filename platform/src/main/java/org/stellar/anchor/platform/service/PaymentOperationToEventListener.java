package org.stellar.anchor.platform.service;

import static org.stellar.anchor.model.TransactionStatus.ERROR;

import com.google.gson.Gson;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.stellar.anchor.event.EventService;
import org.stellar.anchor.event.models.*;
import org.stellar.anchor.exception.SepException;
import org.stellar.anchor.model.Sep31Transaction;
import org.stellar.anchor.model.TransactionStatus;
import org.stellar.anchor.platform.paymentobserver.ObservedPayment;
import org.stellar.anchor.platform.paymentobserver.PaymentListener;
import org.stellar.anchor.server.data.JdbcSep31TransactionStore;
import org.stellar.anchor.util.Log;
import org.stellar.platform.apis.shared.Amount;

@Component
public class PaymentOperationToEventListener implements PaymentListener {
  final JdbcSep31TransactionStore transactionStore;
  final EventService eventService;

  PaymentOperationToEventListener(
      JdbcSep31TransactionStore transactionStore, EventService eventService) {
    this.transactionStore = transactionStore;
    this.eventService = eventService;
  }

  @Override
  public void onReceived(ObservedPayment payment) {
    // Check if payment is connected to a transaction
    if (Objects.toString(payment.getTransactionHash(), "").isEmpty()
        || Objects.toString(payment.getTransactionMemo(), "").isEmpty()) {
      return;
    }

    // Check if the payment contains the expected asset type
    if (!List.of("credit_alphanum4", "credit_alphanum12").contains(payment.getAssetType())) {
      // Asset type does not match
      Log.warn("Not an issued asset");
      return;
    }

    // Find a transaction matching the memo
    Sep31Transaction txn;
    try {
      txn = transactionStore.findByStellarMemo(payment.getTransactionMemo());
    } catch (RuntimeException e) {
      Log.error(
          String.format(
              "error finding transaction that matches the memo (%s).",
              payment.getTransactionMemo()));
      e.printStackTrace();
      return;
    }
    if (txn == null) {
      Log.info(
          String.format(
              "Not expecting any transaction with the memo %s.", payment.getTransactionMemo()));
      return;
    }

    // Compare asset code
    if (!txn.getAmountInAsset().equals(payment.getAssetCode())) {
      Log.warn(
          String.format(
              "Payment asset %s does not match the expected asset %s",
              payment.getAssetCode(), txn.getAmountInAsset()));
      return;
    }

    // Check if the payment contains the expected amount (or greater)
    BigDecimal expectedAmount = new BigDecimal(txn.getAmountIn());
    BigDecimal gotAmount = new BigDecimal(payment.getAmount());
    if (gotAmount.compareTo(expectedAmount) < 0) {
      Log.warn(
          String.format(
              "Payment amount %s is smaller than the expected amount %s",
              payment.getAmount(), txn.getAmountIn()));
      txn.setStatus(ERROR.getName());
      saveTransaction(txn);
      return;
    }

    // Set the transaction status.
    TransactionEvent event = receivedPaymentToEvent(txn, payment);
    if (txn.getStatus().equals(TransactionStatus.PENDING_SENDER.toString())) {
      txn.setStatus(TransactionStatus.PENDING_RECEIVER.toString());
      try {
        transactionStore.save(txn);
      } catch (SepException ex) {
        Log.errorEx(ex);
      }
    }
    // send to the event queue
    sendToQueue(event);
  }

  @Override
  public void onSent(ObservedPayment payment) {
    // noop
  }

  private void sendToQueue(TransactionEvent event) {
    eventService.publish(event);
    Log.info("Sent to event queue" + new Gson().toJson(event));
  }

  TransactionEvent receivedPaymentToEvent(Sep31Transaction txn, ObservedPayment payment) {
    // TODO move event models to /shared
    TransactionEvent event =
        TransactionEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .type(TransactionEvent.Type.TRANSACTION_PAYMENT_RECEIVED)
            .id(txn.getId())
            .status(TransactionEvent.Status.PENDING_RECEIVER)
            .sep(TransactionEvent.Sep.SEP_31)
            .kind(TransactionEvent.Kind.RECEIVE)
            .amountIn(new Amount(payment.getAmount(), txn.getAmountInAsset()))
            .amountOut(new Amount(txn.getAmountOut(), txn.getAmountOutAsset()))
            // TODO: fix PATCH transaction fails if getAmountOut is null?
            .amountFee(new Amount(txn.getAmountFee(), txn.getAmountFeeAsset()))
            .quoteId(txn.getQuoteId())
            .startedAt(txn.getStartedAt())
            .sourceAccount(payment.getFrom())
            .destinationAccount(payment.getTo())
            .creator(
                StellarId.builder()
                    .account(payment.getFrom())
                    .memo(txn.getStellarMemo())
                    .memoType(txn.getStellarMemoType())
                    .build())
            .stellarTransactions(
                new StellarTransaction[] {
                  StellarTransaction.builder()
                      .id(payment.getTransactionHash())
                      .memo(txn.getStellarMemo())
                      .memoType(txn.getStellarMemoType())
                      .createdAt(
                          DateTimeFormatter.ISO_INSTANT.parse(
                              payment.getCreatedAt(), Instant::from))
                      .envelope(payment.getTransactionEnvelope())
                      .payments(
                          new Payment[] {
                            Payment.builder()
                                .operationId(payment.getId())
                                .sourceAccount(payment.getFrom())
                                .destinationAccount(payment.getTo())
                                .amount(new Amount(payment.getAmount(), payment.getAssetName()))
                                .build()
                          })
                      .build()
                })
            .build();
    return event;
  }

  void saveTransaction(Sep31Transaction txn) {
    try {
      transactionStore.save(txn);
    } catch (SepException ex) {
      Log.errorEx(ex);
    }
  }
}

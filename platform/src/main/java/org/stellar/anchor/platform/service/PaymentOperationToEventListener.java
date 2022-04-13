package org.stellar.anchor.platform.service;

import com.google.gson.Gson;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;
import org.stellar.anchor.exception.SepException;
import org.stellar.anchor.model.Sep31Transaction;
import org.stellar.anchor.model.TransactionStatus;
import org.stellar.anchor.platform.paymentobserver.*;
import org.stellar.anchor.server.data.JdbcSep31TransactionStore;
import org.stellar.anchor.util.Log;
import org.stellar.platform.apis.shared.Amount;

@Component
public class PaymentOperationToEventListener implements PaymentListener {
  final JdbcSep31TransactionStore transactionStore;

  PaymentOperationToEventListener(JdbcSep31TransactionStore transactionStore) {
    this.transactionStore = transactionStore;
  }

  @Override
  public void onReceived(ObservedPayment payment) {
    // Check if payment is connected to a transaction
    if (Objects.toString(payment.getTransactionHash(), "").isEmpty()) {
      return;
    }

    // Find a transaction matching the memo
    Sep31Transaction txn;
    try {
      txn = transactionStore.findByStellarMemo(payment.getTransactionMemo());
    } catch (SepException e) {
      Log.error(
          String.format(
              "error finding transaction that matches the memo (%s).",
              payment.getTransactionMemo()));
      e.printStackTrace();
      return;
    }
    if (txn == null) {
      Log.info(String.format("Not expecting any transaction with the memo %s.", payment.getTransactionMemo()));
      return;
    }

    // Check if the payment contains the expected asset
    if (!List.of("credit_alphanum4", "credit_alphanum12").contains(payment.getAssetType())) {
      // Asset type does not match
      Log.warn("Not an issued asset");
      return;
    }
    if (!txn.getAmountInAsset().equals(payment.getAssetCode())) {
      Log.warn(
          String.format(
              "Payment asset %s does not match the expected asset %s",
              payment.getAssetCode(), txn.getAmountInAsset()));
      return;
    }
    // TODO: match asset code.

    // Check if the payment contains the expected amount (or greater)
    BigDecimal expectedAmount = new BigDecimal(txn.getAmountIn());
    BigDecimal gotAmount = new BigDecimal(payment.getAmount());
    if (gotAmount.compareTo(expectedAmount) < 0) {
      Log.warn(String.format("Payment amount %s is smaller than the expected amount %s", payment.getAmount(), txn.getAmountIn()));
      return;
    }

    // Set the transaction status.
    TransactionEvent event = receivedPaymentToEvent(txn, payment);
    if (txn.getStatus().equals(TransactionStatus.PENDING_SENDER.toString())) {
      txn.setStatus(TransactionStatus.PENDING_RECEIVER.toString());
      txn.setStatus(
          TransactionStatus.COMPLETED.toString()); // TODO: remove after event API is implemented.
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
    // TODO: Send the event to event API.
    System.out.println("Sent to event queue" + new Gson().toJson(event));
  }

  TransactionEvent receivedPaymentToEvent(Sep31Transaction txn, ObservedPayment payment) {
    return TransactionEvent.builder()
        .transactionId(txn.getId())
        .status(txn.getStatus())
        .amountIn(new Amount(payment.getAmount(), txn.getAmountInAsset()))
        .amountOut(new Amount(txn.getAmountOut(), txn.getAmountOutAsset()))
        .amountFee(new Amount(txn.getAmountFee(), txn.getAmountFeeAsset()))
        .stellarTransactions(
            StellarTransaction.builder()
                .id(txn.getStellarTransactionId())
                .memo(txn.getStellarMemo())
                .memoType(txn.getStellarMemoType())
                .createdAt(
                    DateTimeFormatter.ISO_INSTANT.parse(payment.getCreatedAt(), Instant::from))
                .envelope(payment.getTransactionEnvelope())
                .payment(
                    StellarPayment.builder()
                        .operationId(payment.getId())
                        .sourceAccount(payment.getFrom())
                        .destinationAccount(payment.getTo())
                        .amount(new Amount(payment.getAmount(), payment.getAssetName()))
                        .build())
                .build())
        .build();
  }
}

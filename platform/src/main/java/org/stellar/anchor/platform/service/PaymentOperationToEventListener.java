package org.stellar.anchor.platform.service;

import com.google.gson.Gson;
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
    if (Objects.toString(payment.getTransactionHash(), "").isEmpty()) {
      return;
    }

    // Find the matching transaction
    Sep31Transaction txn = null;
    try {
      txn = transactionStore.findByStellarMemo(payment.getTransactionMemo());
    } catch (SepException e) {
      Log.error(
          String.format(
              "error finding transaction that matches the memo (%s).",
              payment.getTransactionMemo()));
      e.printStackTrace();
    }

    if (txn == null) {
      Log.info(String.format("no transaction(stellarAccountId=%s) is found.", payment.getTo()));
      return;
    }

    if (!List.of("credit_alphanum4", "credit_alphanum12").contains(payment.getAssetType())) {
      // Asset type does not match
      Log.warn("Not an issued asset");
      return;
    }

    if (!txn.getAmountInAsset().equals(payment.getAssetCode())) {
      Log.warn(
          String.format(
              "Payment asset(%s) does not match the expected asset(%s)",
              payment.getAssetCode(), txn.getAmountInAsset()));
      return;
    }

    // convert to event
    TransactionEvent event = receivedPaymentToEvent(txn, payment);
    // Set the transaction status.
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
    // send to event queue
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
    TransactionEvent txnEvent =
        TransactionEvent.builder()
            .transactionId(txn.getId())
            .status(txn.getStatus())
            .amountIn(new Amount(txn.getAmountIn(), txn.getAmountInAsset()))
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
    // Assign values from the payment
    txnEvent.getAmountIn().setAmount(payment.getAmount());
    return txnEvent;
  }
}

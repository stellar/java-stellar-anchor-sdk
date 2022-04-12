package org.stellar.anchor.platform.service;

import com.google.gson.Gson;
import org.springframework.stereotype.Component;
import org.stellar.anchor.exception.SepException;
import org.stellar.anchor.model.Sep31Transaction;
import org.stellar.anchor.model.TransactionStatus;
import org.stellar.anchor.platform.paymentobserver.PaymentListener;
import org.stellar.anchor.platform.paymentobserver.StellarTransaction;
import org.stellar.anchor.platform.paymentobserver.TransactionEvent;
import org.stellar.anchor.server.data.JdbcSep31TransactionStore;
import org.stellar.anchor.util.Log;
import org.stellar.platform.apis.shared.Amount;
import org.stellar.sdk.AssetTypeCreditAlphaNum;
import org.stellar.sdk.responses.operations.PaymentOperationResponse;

@Component
public class PaymentOperationToEventListener implements PaymentListener {
  final JdbcSep31TransactionStore transactionStore;

  PaymentOperationToEventListener(JdbcSep31TransactionStore transactionStore) {
    this.transactionStore = transactionStore;
  }

  @Override
  public void onReceived(PaymentOperationResponse payment) {
    // Find the matching transaction
    Sep31Transaction txn = transactionStore.findByStellarAccountId(payment.getTo());
    if (txn == null) {
      Log.error(String.format("no transaction(stellarAccountId=%s) is found.", payment.getTo()));
      return;
    }

    if (!(payment.getAsset() instanceof AssetTypeCreditAlphaNum)) {
      // Asset does not match
      return;
    }

    AssetTypeCreditAlphaNum atcPayment = (AssetTypeCreditAlphaNum) payment.getAsset();
    if (!txn.getAmountInAsset().equals(atcPayment.getCode())) {
      Log.error(
          String.format(
              "Payment asset(%s) does not match the expected asset(%s)",
              atcPayment.getCode(), txn.getAmountInAsset()));
      return;
    }

    // convert to event
    TransactionEvent event = receivedPaymentToEvent(txn, payment);
    // Set the transaction status.
    if (txn.getStatus().equals(TransactionStatus.PENDING_SENDER.toString())) {
      txn.setStatus(TransactionStatus.PENDING_RECEIVER.toString());
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
  public void onSent(PaymentOperationResponse payment) {
    // noop
  }

  private void sendToQueue(TransactionEvent event) {
    // TODO: Send the event to event API.
    System.out.println("Sent to event queue" + new Gson().toJson(event));
  }

  TransactionEvent receivedPaymentToEvent(Sep31Transaction txn, PaymentOperationResponse payment) {
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

                    // TODO: payment does not provide access to createdAt timestamp. Need to submit
                    // a PR.
                    .build())
            .build();
    // Assign values from the payment
    txnEvent.getAmountIn().setAmount(payment.getAmount());
    return txnEvent;
  }
}

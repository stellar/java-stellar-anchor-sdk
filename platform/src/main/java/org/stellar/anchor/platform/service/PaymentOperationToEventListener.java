package org.stellar.anchor.platform.service;

import com.google.gson.Gson;
import org.apache.tomcat.util.codec.binary.Base64;
import org.springframework.stereotype.Component;
import org.stellar.anchor.event.EventService;
import org.stellar.anchor.event.models.Amount;
import org.stellar.anchor.event.models.StellarId;
import org.stellar.anchor.event.models.TransactionEvent;
import org.stellar.anchor.event.models.StellarTransaction;
import org.stellar.anchor.exception.SepException;
import org.stellar.anchor.model.Sep31Transaction;
import org.stellar.anchor.model.TransactionStatus;
import org.stellar.anchor.platform.paymentobserver.PaymentListener;
import org.stellar.anchor.server.data.JdbcSep31TransactionStore;
import org.stellar.anchor.util.Log;
import org.stellar.sdk.AssetTypeCreditAlphaNum;
import org.stellar.sdk.MemoHash;
import org.stellar.sdk.responses.operations.PaymentOperationResponse;

import java.util.UUID;

@Component
public class PaymentOperationToEventListener implements PaymentListener {
  final JdbcSep31TransactionStore transactionStore;
  final EventService eventService;

  PaymentOperationToEventListener(JdbcSep31TransactionStore transactionStore,
                                  EventService eventService) {
    this.transactionStore = transactionStore;
    this.eventService = eventService;
  }

  @Override
  public void onReceived(PaymentOperationResponse payment) {
    if (!payment.getTransaction().isPresent()) {
      return;
    }

    MemoHash memoHash = (MemoHash) payment.getTransaction().get().getMemo();
    String hash = new String(Base64.encodeBase64(memoHash.getBytes()));

    // Find the matching transaction
    Sep31Transaction txn = null;
    try {
      txn = transactionStore.findByStellarMemo(hash);
    } catch (SepException e) {
      Log.info(String.format("error finding transaction that matches the memo (%s).", hash));
    }

    if (txn == null) {
      Log.info(String.format("no transaction(stellarAccountId=%s) is found.", payment.getTo()));
      return;
    }

    if (!(payment.getAsset() instanceof AssetTypeCreditAlphaNum)) {
      // Asset does not match. Ignore.
      Log.info(String.format("unexpected payment type %s", payment.getAsset().getClass()));
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
  public void onSent(PaymentOperationResponse payment) {
    // noop
  }

  private void sendToQueue(TransactionEvent event) {
    eventService.publish(event);
    System.out.println("Sent to event queue" + new Gson().toJson(event));
  }

  TransactionEvent receivedPaymentToEvent(Sep31Transaction txn, PaymentOperationResponse payment) {
    TransactionEvent event = TransactionEvent.builder()
      .eventId(UUID.randomUUID().toString())
      .type(org.stellar.anchor.event.models.TransactionEvent.Type.TRANSACTION_CREATED)
      .id(txn.getId())
      .status(TransactionEvent.Status.valueOf(txn.getStatus()))
      .sep(org.stellar.anchor.event.models.TransactionEvent.Sep.SEP_31)
      .kind(org.stellar.anchor.event.models.TransactionEvent.Kind.RECEIVE)
      .amountIn(org.stellar.anchor.event.models.Amount.builder()
              .amount(txn.getAmountIn())
              .asset(txn.getAmountInAsset())
              .build()
      )
      .amountOut(org.stellar.anchor.event.models.Amount.builder()
              .amount(txn.getAmountOut())
              .asset(txn.getAmountInAsset())
              .build()
      )
      .amountFee(Amount.builder()
              .amount(txn.getAmountFee())
              .asset(txn.getAmountInAsset())
              .build()
      )
      .quoteId(txn.getQuoteId())
      .startedAt(txn.getStartedAt())
      .sourceAccount(payment.getSourceAccount())
      .destinationAccount(payment.getTo())
      .creator(
         StellarId.builder()
            .account(txn.getStellarAccountId())
            .memo(txn.getStellarMemo())
            .memoType(txn.getStellarMemoType())
            .build()
      )
      .stellarTransactions(new StellarTransaction[]{
         StellarTransaction.builder()
            .id(txn.getStellarTransactionId())
            .memo(txn.getStellarMemo())
            .memoType(txn.getStellarMemoType())
            // TODO: payment does not provide access to createdAt timestamp. Need to submit
            // a PR.
            .build()})
      .build();
    // Assign values from the payment
    event.getAmountIn().setAmount(payment.getAmount());
    return event;
  }
}

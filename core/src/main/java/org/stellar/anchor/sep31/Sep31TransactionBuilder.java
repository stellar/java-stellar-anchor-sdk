package org.stellar.anchor.sep31;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.stellar.anchor.api.sep.AssetInfo;
import org.stellar.anchor.event.models.StellarId;

public class Sep31TransactionBuilder {
  final Sep31Transaction txn;
  final Sep31TransactionStore factory;

  public Sep31TransactionBuilder(Sep31TransactionStore factory) {
    this.factory = factory;
    this.txn = factory.newTransaction();
  }

  public Sep31TransactionBuilder id(String id) {
    txn.setId(id);
    return this;
  }

  public Sep31TransactionBuilder status(String status) {
    txn.setStatus(status);
    return this;
  }

  public Sep31TransactionBuilder statusEta(Long statusEta) {
    txn.setStatusEta(statusEta);
    return this;
  }

  public Sep31TransactionBuilder amountIn(String amountIn) {
    txn.setAmountIn(amountIn);
    return this;
  }

  public Sep31TransactionBuilder amountInAsset(String amountInAsset) {
    txn.setAmountInAsset(amountInAsset);
    return this;
  }

  public Sep31TransactionBuilder amountOut(String amountOut) {
    txn.setAmountOut(amountOut);
    return this;
  }

  public Sep31TransactionBuilder amountOutAsset(String amountOutAsset) {
    txn.setAmountOutAsset(amountOutAsset);
    return this;
  }

  public Sep31TransactionBuilder amountFee(String amountFee) {
    txn.setAmountFee(amountFee);
    return this;
  }

  public Sep31TransactionBuilder amountFeeAsset(String amountFeeAsset) {
    txn.setAmountFeeAsset(amountFeeAsset);
    return this;
  }

  public Sep31TransactionBuilder stellarAccountId(String stellarAccountId) {
    txn.setStellarAccountId(stellarAccountId);
    return this;
  }

  public Sep31TransactionBuilder stellarMemo(String stellarMemo) {
    txn.setStellarMemo(stellarMemo);
    return this;
  }

  public Sep31TransactionBuilder stellarMemoType(String stellarMemoType) {
    txn.setStellarMemoType(stellarMemoType);
    return this;
  }

  public Sep31TransactionBuilder startedAt(Instant startedAt) {
    txn.setStartedAt(startedAt);
    return this;
  }

  public Sep31TransactionBuilder completedAt(Instant completedAt) {
    txn.setCompletedAt(completedAt);
    return this;
  }

  public Sep31TransactionBuilder stellarTransactionId(String stellarTransactionId) {
    txn.setStellarTransactionId(stellarTransactionId);
    return this;
  }

  public Sep31TransactionBuilder externalTransactionId(String externalTransactionId) {
    txn.setExternalTransactionId(externalTransactionId);
    return this;
  }

  public Sep31TransactionBuilder refunded(Boolean refunded) {
    txn.setRefunded(refunded);
    return this;
  }

  public Sep31TransactionBuilder refunds(Sep31Transaction.Refunds refunds) {
    txn.setRefunds(refunds);
    return this;
  }

  public Sep31TransactionBuilder requiredInfoMessage(String requiredInfoMessage) {
    txn.setRequiredInfoMessage(requiredInfoMessage);
    return this;
  }

  public Sep31TransactionBuilder fields(Map<String, String> fields) {
    txn.setFields(fields);
    return this;
  }

  public Sep31TransactionBuilder requiredInfoUpdates(
      AssetInfo.Sep31TxnFieldSpecs requiredInfoUpdates) {
    txn.setRequiredInfoUpdates(requiredInfoUpdates);
    return this;
  }

  public Sep31TransactionBuilder quoteId(String quoteId) {
    txn.setQuoteId(quoteId);
    return this;
  }

  public Sep31TransactionBuilder clientDomain(String clientDomain) {
    txn.setClientDomain(clientDomain);
    return this;
  }

  public Sep31TransactionBuilder senderId(String senderId) {
    txn.setSenderId(senderId);
    return this;
  }

  public Sep31TransactionBuilder receiverId(String receiverId) {
    txn.setReceiverId(receiverId);
    return this;
  }

  public Sep31TransactionBuilder creator(StellarId creator) {
    txn.setCreator(creator);
    return this;
  }

  public Sep31Transaction build() {
    return txn;
  }

  public class RefundsBuilder {
    Sep31Transaction.Refunds refunds;

    RefundsBuilder() {
      refunds = factory.newRefunds();
    }

    RefundsBuilder amountRefunded(String amountRefunded) {
      refunds.setAmountRefunded(amountRefunded);
      return this;
    }

    RefundsBuilder amountFee(String amountFee) {
      refunds.setAmountFee(amountFee);
      return this;
    }

    RefundsBuilder payments(List<Sep31Transaction.RefundPayment> payments) {
      refunds.setRefundPayments(payments);
      return this;
    }
  }

  public class RefundPaymentBuilder {
    Sep31Transaction.RefundPayment refundPayment;

    RefundPaymentBuilder() {
      refundPayment = factory.newRefundPayment();
    }

    RefundPaymentBuilder id(String id) {
      refundPayment.setId(id);
      return this;
    }

    RefundPaymentBuilder amount(String amount) {
      refundPayment.setAmount(amount);
      return this;
    }

    RefundPaymentBuilder fee(String fee) {
      refundPayment.setFee(fee);
      return this;
    }
  }
}

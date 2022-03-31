package org.stellar.anchor.model;

import org.stellar.anchor.asset.AssetInfo;

import java.time.Instant;
import java.util.List;

public interface Sep31Transaction {
  String getId();

  void setId(String id);

  String getStatus();

  void setStatus(String status);

  Long getStatusEta();

  void setStatusEta(Long statusEta);

  String getAmountIn();

  void setAmountIn(String amountIn);

  String getAmountInAsset();

  void setAmountInAsset(String amountInAsset);

  String getAmountOut();

  void setAmountOut(String amountOut);

  String getAmountOutAsset();

  void setAmountOutAsset(String amountOutAsset);

  String getAmountFee();

  void setAmountFee(String amountFee);

  String getAmountFeeAsset();

  void setAmountFeeAsset(String amountFeeAsset);

  String getStellarAccountId();

  void setStellarAccountId(String stellarAccountId);

  String getStellarMemo();

  void setStellarMemo(String stellarMemo);

  String getStellarMemoType();

  void setStellarMemoType(String stellarMemoType);

  Instant getStartedAt();

  void setStartedAt(Instant startedAt);

  Instant getCompletedAt();

  void setCompletedAt(Instant completedAt);

  String getStellarTransactionId();

  void setStellarTransactionId(String stellarTransactionId);

  String getExternalTransactionId();

  void setExternalTransactionId(String externalTransactionId);

  Boolean getRefunded();

  void setRefunded(Boolean refunded);

  Refunds getRefunds();

  void setRefunds(Refunds refunds);

  String getRequiredInfoMessage();

  void setRequiredInfoMessage(String requiredInfoMessage);

  AssetInfo.Sep31TxnFields getRequiredInfoUpdates();

  void setRequiredInfoUpdates(AssetInfo.Sep31TxnFields requiredInfoUpdates);

  String getQuoteId();

  void setQuoteId(String quoteId);

  String getClientDomain();

  void setClientDomain(String clientDomain);

  interface Refunds {

    String getAmountRefunded();

    void setAmountRefunded(String amountRefunded);

    String getAmountFee();

    void setAmountFee(String amountFee);

    List<RefundPayment> getRefundPayments();

    void setRefundPayments(List<RefundPayment> refundPayments);
  }

  interface RefundPayment {
    String getId();

    void setId(String id);

    String getAmount();

    void setAmount(String amount);

    String getFee();

    void setFee(String fee);
  }
}

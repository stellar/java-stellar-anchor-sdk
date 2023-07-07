package org.stellar.anchor.sep6;

import java.time.Instant;

public interface Sep6Transaction {

  String getId();

  void setId(String id);

  String getTransactionId();

  void setTransactionId(String transactionId);

  String getStellarTransactionId();

  void setStellarTransactionId(String stellarTransactionId);

  String getExternalTransactionId();

  void setExternalTransactionId(String externalTransactionId);

  String getStatus();

  void setStatus(String status);

  String getKind();

  void setKind(String kind);

  Instant getStartedAt();

  void setStartedAt(Instant startedAt);

  Instant getCompletedAt();

  void setCompletedAt(Instant completedAt);

  String getRequestAssetCode();

  void setRequestAssetCode(String assetCode);

  String getRequestAssetIssuer();

  void setRequestAssetIssuer(String assetIssuer);

  String getAmountExpected();

  void setAmountExpected(String amount);

  String getSep10Account();

  void setSep10Account(String sep10Account);

  String getSep10AccountMemo();

  void setSep10AccountMemo(String sep10AccountMemo);

  String getWithdrawAnchorAccount();

  void setWithdrawAnchorAccount(String withdrawAnchorAccount);

  // TODO: are these even needed?
  String getFromAccount();

  void setFromAccount(String fromAccount);

  String getToAccount();

  void setToAccount(String toAccount);

  String getMemo();

  void setMemo(String memo);

  String getMemoType();

  void setMemoType(String memoType);

  Boolean getClaimableBalanceSupported();

  void setClaimableBalanceSupported(Boolean claimableBalanceSupported);

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

  Boolean getRefunded();

  void setRefunded(Boolean refunded);

  Sep6Refunds getRefunds();

  void setRefunds(Sep6Refunds refunds);

  String getRefundMemo();

  void setRefundMemo(String refundMemo);

  String getRefundMemoType();

  void setRefundMemoType(String refundMemoType);

  enum Kind {
    DEPOSIT("deposit"),
    WITHDRAWAL("withdrawal"),

    // TODO: This is how it is represented in Polaris
    DEPOSIT_EXCHANGE("deposit-exchange"),
    WITHDRAWAL_EXCHANGE("withdrawal-exchange");

    private final String name;

    Kind(String name) {
      this.name = name;
    }

    @Override
    public String toString() {
      return name;
    }
  }
}

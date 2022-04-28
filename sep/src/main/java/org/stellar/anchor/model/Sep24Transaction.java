package org.stellar.anchor.model;

@SuppressWarnings("unused")
public interface Sep24Transaction {
  String getId();

  void setId(String id);

  String getDocumentType();

  void setDocumentType(String documentType);

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

  Long getStartedAt();

  void setStartedAt(Long startedAt);

  Long getCompletedAt();

  void setCompletedAt(Long completedAt);

  String getAssetCode();

  void setAssetCode(String assetCode);

  String getAssetIssuer();

  void setAssetIssuer(String assetIssuer);

  String getStellarAccount();

  void setStellarAccount(String stellarAccount);

  String getReceivingAnchorAccount();

  void setReceivingAnchorAccount(String receivingAnchorAccount);

  String getFromAccount();

  void setFromAccount(String fromAccount);

  String getToAccount();

  void setToAccount(String toAccount);

  String getMemoType();

  void setMemoType(String memoType);

  String getMemo();

  void setMemo(String memo);

  String getProtocol();

  void setProtocol(String protocol);

  String getDomainClient();

  void setDomainClient(String domainClient);

  Boolean getClaimableBalanceSupported();

  void setClaimableBalanceSupported(Boolean claimableBalanceSupported);

  String getAmountIn();

  void setAmountIn(String amountIn);

  String getAmountOut();

  void setAmountOut(String amountOut);

  String getAmountFee();

  void setAmountFee(String amountFee);

  String getAmountInAsset();

  void setAmountInAsset(String amountInAsset);

  String getAmountOutAsset();

  void setAmountOutAsset(String amountOutAsset);

  String getAmountFeeAsset();

  void setAmountFeeAsset(String amountFeeAsset);

  String getAccountMemo();

  void setAccountMemo(String accountMemo);

  String getMuxedAccount();

  void setMuxedAccount(String muxedAccount);

  enum Kind {
    DEPOSIT("deposit"),
    WITHDRAWAL("withdrawal"),
    SEND("send");
    private final String name;

    Kind(String name) {
      this.name = name;
    }

    public String toString() {
      return name;
    }
  }

  enum Protocol {
    SEP6("sep6"),
    SEP24("sep24"),
    SEP31("sep31");

    private final String name;

    Protocol(String name) {
      this.name = name;
    }

    public String toString() {
      return name;
    }
  }
}

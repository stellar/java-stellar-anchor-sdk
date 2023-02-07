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

  // The account from the JWT.
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

  // From JWT account memo
  String getAccountMemo();

  void setAccountMemo(String accountMemo);

  String getMuxedAccount();

  void setMuxedAccount(String muxedAccount);

  String getRefundMemo();

  void setRefundMemo(String refundMemo);

  String getRefundMemoType();

  void setRefundMemoType(String refundMemoType);

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

  enum Status {
    PENDING_ANCHOR("pending_anchor", "processing"),
    PENDING_TRUST("pending_trust", "waiting for a trustline to be established"),
    PENDING_USER("pending_user", "waiting on user action"),
    PENDING_USR_TRANSFER_START(
        "pending_user_transfer_start", "waiting on the user to transfer funds"),
    PENDING_USR_TRANSFER_COMPLETE(
        "pending_user_transfer_complete", "the user has transferred the funds"),
    INCOMPLETE("incomplete", "incomplete"),
    NO_MARKET("no_market", "no market for the asset"),
    TOO_SMALL("too_small", "the transaction amount is too small"),
    TOO_LARGE("too_large", "the transaction amount is too big"),
    PENDING_SENDER("pending_sender", null),
    PENDING_RECEIVER("pending_receiver", null),
    PENDING_TRANSACTION_INFO_UPDATE("pending_transaction_info_update", null),
    PENDING_CUSTOMER_INFO_UPDATE("pending_customer_info_update", null),
    REFUNDED("refunded", "the deposit/withdrawal is fully refunded"),
    COMPLETED("completed", "complete"),
    ERROR("error", "error"),
    PENDING_EXTERNAL("pending_external", "waiting on an external entity"),
    PENDING_STELLAR("pending_stellar", "stellar is executing the transaction");

    private final String name;
    private final String description;

    Status(String name, String description) {
      this.name = name;
      this.description = description;
    }

    public String toString() {
      return name;
    }

    public String getDescription() {
      return description;
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

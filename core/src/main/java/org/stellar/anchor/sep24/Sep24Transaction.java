package org.stellar.anchor.sep24;

import java.time.Instant;

@SuppressWarnings("unused")
public interface Sep24Transaction {

  /**
   * The database ID.
   *
   * @return The generated database ID.
   */
  String getId();

  void setId(String id);

  /**
   * Unique, anchor-generated id for the transaction.
   *
   * @return The <code>id</code> field of the SEP-24 transaction history.
   */
  String getTransactionId();

  void setTransactionId(String transactionId);

  /**
   * <code>transaction_id</code> on Stellar network of the transfer that either completed the
   * deposit or started the withdrawal.
   *
   * @return The <code>stellar_transction_id</code> field of the SEP-24 transaction history.
   */
  String getStellarTransactionId();

  void setStellarTransactionId(String stellarTransactionId);

  /**
   * ID of transaction on external network that either started the deposit or completed the
   * withdrawal.
   *
   * @return The <code>external_transction_id</code> field of the SEP-24 transaction history.
   */
  String getExternalTransactionId();

  void setExternalTransactionId(String externalTransactionId);

  /**
   * Processing status of deposit/withdrawal.
   *
   * @return The <code>status</code> field of the SEP-24 transaction history.
   */
  String getStatus();

  void setStatus(String status);

  /**
   * <code>deposit</code> or <code>withdrawal</code> .
   *
   * @return The <code>kind</code> field of the SEP-24 transaction history.
   */
  String getKind();

  void setKind(String kind);

  /**
   * Start date and time of transaction.
   *
   * @return The <code>started_at</code> field of the SEP-24 transaction history.
   */
  Instant getStartedAt();

  void setStartedAt(Instant startedAt);

  /**
   * The date and time of transaction reaching <code>completed</code> or <code>refunded</code>
   * status.
   *
   * @return <code>completed</code> field of the SEP-24 transaction history.
   */
  Instant getCompletedAt();

  void setCompletedAt(Instant completedAt);

  /**
   * The code of the asset of interest. E.g. BTC, ETH, USD, INR, etc.
   *
   * @return <code>asset_code</code> field of the SEP-24 transaction history.
   */
  String getRequestAssetCode();

  void setRequestAssetCode(String assetCode);

  /**
   * The issuer of the stellar asset the user wants to receive for their deposit with the anchor. If
   * asset_issuer is not provided, the anchor should use the asset issued by themselves as described
   * in their TOML file.
   *
   * @return the asset issuer of the transaction's <code>asset_code</code> .
   */
  String getRequestAssetIssuer();

  void setRequestAssetIssuer(String assetIssuer);

  /** Amount requested by the user as a string with up to 7 decimals. */
  String getRequestedAmount();

  void setRequestedAmount(String amount);

  /**
   * The Stellar account used to authenticate SEP-10;.
   *
   * @return the stellar account.
   */
  String getSep10Account();

  void setSep10Account(String sep10Account);

  /**
   * If this is a withdrawal, this is the anchor's Stellar account that the user transferred (or
   * will transfer) their issued asset to.
   *
   * @return <code>withdraw_anchor_account</code> field of the SEP-24 transaction history.
   */
  String getWithdrawAnchorAccount();

  void setWithdrawAnchorAccount(String withdrawAnchorAccount);

  /**
   * If withdrawal, the Stellar account the assets were withdrawn from.
   *
   * <p>If deposit, sent from address, perhaps BTC, IBAN, or bank account.
   *
   * @return <code>from</code> field of the SEP-24 transaction history.
   */
  String getFromAccount();

  void setFromAccount(String fromAccount);

  /**
   * If withdrawal, sent to address (perhaps BTC, IBAN, or bank account in the case of a withdrawal,
   * Stellar account in the case of a deposit).
   *
   * <p>If deposit, the Stellar account the deposited assets were sent to.
   *
   * @return <code>to</code> field of the SEP-24 transaction history.
   */
  String getToAccount();

  void setToAccount(String toAccount);

  /**
   * If withdrawal, this is the memo used when the user transferred to withdraw_anchor_account.
   * Assigned null if the withdraw is not ready to receive payment, for example if KYC is not
   * completed.
   *
   * <p>If deposit, this is the memo (if any) used to transfer the asset to the to Stellar address
   *
   * @return <code>withdraw_memo</code> or <code>deposit_memo</code> of the SEP-24 transaction
   *     history.
   */
  String getMemo();

  void setMemo(String memo);

  /**
   * Type for the <code>memo</code> field.
   *
   * @return <code>withdraw_memo_type</code> or <code>deposit_memo_type</code> of the SEP-24
   *     transaction history.
   */
  String getMemoType();

  void setMemoType(String memoType);

  /**
   * The client domain of the wallet that initiated the transaction.
   *
   * @return the client domain.
   */
  String getClientDomain();

  void setClientDomain(String domainClient);

  /**
   * True if the client supports receiving deposit transactions as a claimable balance, false
   * otherwise.
   *
   * @return <code>true</code> or <code>false</code>
   */
  Boolean getClaimableBalanceSupported();

  void setClaimableBalanceSupported(Boolean claimableBalanceSupported);

  /**
   * Amount received by anchor at start of transaction as a string with up to 7 decimals. Excludes
   * any fees charged before the anchor received the funds.
   *
   * @return <code>amount_in</code> field of the SEP-24 transaction history.
   */
  String getAmountIn();

  void setAmountIn(String amountIn);

  /**
   * The asset received or to be received by the Anchor. Must be present if the deposit/withdraw was
   * made using non-equivalent assets. The value must be in SEP-38 Asset Identification Format. See
   * the Asset Exchanges section for more information.
   *
   * @return <code>amount_in_asset</code> field of the SEP-24 transaction history.
   */
  String getAmountInAsset();

  void setAmountInAsset(String amountInAsset);

  /**
   * Amount sent by anchor to user at end of transaction as a string with up to 7 decimals. Excludes
   * amount converted to XLM to fund account and any external fees.
   *
   * @return <code>amount_out</code> field of the SEP-24 transaction history.
   */
  String getAmountOut();

  void setAmountOut(String amountOut);

  /**
   * The asset of the <code>amount_out</code> field.
   *
   * @return <code>amount_out_asset</code> field of the SEP-24 transaction history.
   */
  String getAmountOutAsset();

  void setAmountOutAsset(String amountOutAsset);
  /**
   * Amount of fee charged by anchor.
   *
   * @return <code>amount_fee</code> field of the SEP-24 transaction history.
   */
  String getAmountFee();

  void setAmountFee(String amountFee);

  /**
   * The asset in which fees are calculated in. Must be present if the deposit/withdraw was made
   * using non-equivalent assets. The value must be in SEP-38 Asset Identification Format. See the
   * Asset Exchanges section for more information.
   *
   * @return <code>amount_fee_asset</code> field of the SEP-24 transaction history.
   */
  String getAmountFeeAsset();

  void setAmountFeeAsset(String amountFeeAsset);

  Boolean getRefunded();

  void setRefunded(Boolean refunded);

  Sep24Refunds getRefunds();

  void setRefunds(Sep24Refunds refunds);

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

package org.stellar.anchor.platform.data;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.vladmihalcea.hibernate.type.json.JsonType;
import java.time.Instant;
import javax.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.TypeDef;
import org.stellar.anchor.sep24.Sep24Refunds;
import org.stellar.anchor.sep24.Sep24Transaction;
import org.stellar.anchor.util.GsonUtils;

@Getter
@Setter
@Entity
@Access(AccessType.FIELD)
@Table(name = "sep24_transaction")
@TypeDef(name = "json", typeClass = JsonType.class)
public class JdbcSep24Transaction implements Sep24Transaction, SepTransaction {
  static Gson gson = GsonUtils.getInstance();

  @Id String id;

  String kind;

  String status;

  @SerializedName("status_eta")
  String statusEta;

  @SerializedName("kyc_verified")
  String kycVerified;

  @SerializedName("more_info_url")
  String moreInfoUrl;

  @SerializedName("amount_in")
  String amountIn;

  @SerializedName("amount_in_asset")
  String amountInAsset;

  @SerializedName("amount_out")
  String amountOut;

  @SerializedName("amount_out_asset")
  String amountOutAsset;

  @SerializedName("amount_fee")
  String amountFee;

  @SerializedName("amount_fee_asset")
  String amountFeeAsset;

  @SerializedName("started_at")
  Instant startedAt;

  @SerializedName("completed_at")
  Instant completedAt;

  @SerializedName("transaction_id")
  String transactionId;

  @SerializedName("stellar_transaction_id")
  String stellarTransactionId;

  @SerializedName("external_transaction_id")
  String externalTransactionId;

  String message;

  Boolean refunded;

  // Ignored by JPA
  @Transient Sep24Refunds refunds;

  @Access(AccessType.PROPERTY)
  @Column(name = "refunds")
  public String getRefundsJson() {
    return gson.toJson(this.refunds);
  }

  public void setRefundsJson(String refundsJson) {
    if (refundsJson != null) {
      this.refunds = gson.fromJson(refundsJson, JdbcSep24Refunds.class);
    }
  }

  /**
   * If this is a withdrawal, this is the anchor's Stellar account that the user transferred (or
   * will transfer) their issued asset to.
   */
  @SerializedName("withdraw_anchor_account")
  String withdrawAnchorAccount;

  /** The memo for deposit or withdraw */
  String memo;

  /** The memo type of the transaction */
  @SerializedName("memo_type")
  String memoType;

  /**
   * Sent from address.
   *
   * <p>In a deposit transaction, this would be a non-stellar account such as, BTC, IBAN, or bank
   * account.
   *
   * <p>In a withdrawal transaction, this would be the stellar account the assets were withdrawn
   * from.
   */
  @SerializedName("from_account")
  String fromAccount;

  /**
   * Sent to address.
   *
   * <p>In a deposit transaction, this would be a stellar account the assets were deposited to.
   *
   * <p>In a withdrawal transaction, this would be the non-stellar account such as BTC, IBAN, or
   * bank account.
   */
  @SerializedName("to_account")
  String toAccount;

  @SerializedName("request_asset_code")
  String requestAssetCode;

  @SerializedName("request_asset_issuer")
  String requestAssetIssuer;

  /**
   * The SEP10 account used for authentication.
   *
   * <p>The account can be in the format of 1) stellar_account (G...) 2) stellar_account:memo
   * (G...:2810101841641761712) 3) muxed account (M...)
   */
  @SerializedName("sep10_account")
  String sep10Account;

  @SerializedName("client_domain")
  String clientDomain;

  @SerializedName("claimable_balance_supported")
  Boolean claimableBalanceSupported;
}

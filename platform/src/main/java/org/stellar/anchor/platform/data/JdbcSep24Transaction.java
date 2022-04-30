package org.stellar.anchor.platform.data;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.stellar.anchor.sep24.Sep24RefundPayment;
import org.stellar.anchor.sep24.Sep24Transaction;

@Getter
@Setter
@Entity
@Table(name = "sep24_transaction")
public class JdbcSep24Transaction implements Sep24Transaction {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  @Column(name = "sep_transaction_id")
  Long jdbcId;

  String id;

  /** Document Type used for indexing the document. */
  String documentType;

  @SerializedName("transaction_id")
  String transactionId;

  @SerializedName("stellar_transaction_id")
  String stellarTransactionId;

  @SerializedName("external_transaction_id")
  String externalTransactionId;

  String status;

  String kind;

  @SerializedName("started_at")
  Long startedAt;

  @SerializedName("completed_at")
  Long completedAt;

  @SerializedName("asset_code")
  String assetCode; // *

  @SerializedName("asset_issuer")
  String assetIssuer; // *

  @SerializedName("asset_account")
  String stellarAccount; // *

  @SerializedName("receiving_anchor_account")
  String receivingAnchorAccount;

  @SerializedName("from_account")
  String fromAccount; // *

  @SerializedName("to_account")
  String toAccount; // *

  @SerializedName("memo_type")
  String memoType;

  String memo;

  String protocol;

  @SerializedName("domain_client")
  String domainClient;

  @SerializedName("claimable_balance_supported")
  Boolean claimableBalanceSupported;

  @SerializedName("amount_in")
  String amountIn;

  @SerializedName("amount_out")
  String amountOut;

  @SerializedName("amount_fee")
  String amountFee;

  @SerializedName("amount_in_asset")
  String amountInAsset;

  @SerializedName("amount_out_asset")
  String amountOutAsset;

  @SerializedName("amount_fee_asset")
  String amountFeeAsset;

  @SerializedName("account_memo")
  String accountMemo; // *

  @SerializedName("muxed_account")
  String muxedAccount;

  @OneToMany(fetch = FetchType.LAZY, mappedBy = "transaction")
  List<JdbcSep24RefundPayment> refundPayments;

  @Override
  public List<? extends Sep24RefundPayment> getRefundPayments() {
    return refundPayments;
  }

  @Override
  public void setRefundPayments(List<? extends Sep24RefundPayment> payments) {
    refundPayments = new ArrayList<>(payments.size());
    payments.stream()
        .filter(p -> p instanceof JdbcSep24RefundPayment)
        .forEach(fp -> refundPayments.add((JdbcSep24RefundPayment) fp));
  }
}

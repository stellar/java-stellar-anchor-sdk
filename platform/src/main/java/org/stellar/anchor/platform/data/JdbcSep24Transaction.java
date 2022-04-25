package org.stellar.anchor.platform.data;

import com.google.gson.annotations.SerializedName;
import javax.persistence.*;
import lombok.Data;
import org.stellar.anchor.model.Sep24Transaction;

@Data
@Entity
@Table(name = "sep24_transaction")
public class JdbcSep24Transaction implements Sep24Transaction {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
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
}

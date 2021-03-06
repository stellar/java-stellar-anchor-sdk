package org.stellar.anchor.api.sep.sep24;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class TransactionResponse {
  String id;

  String kind;

  String status;

  @SerializedName("status_eta")
  Integer status_eta;

  @SerializedName("more_info_url")
  String moreInfoUrl = "";

  @SerializedName("amount_in")
  String amountIn = "0";

  @SerializedName("amount_in_asset")
  String amountInAsset;

  @SerializedName("amount_out")
  String amountOut = "0";

  @SerializedName("amount_out_asset")
  String amountOutAsset;

  @SerializedName("amount_fee")
  String amountFee = "0";

  @SerializedName("amount_fee_asset")
  String amountFeeAsset;

  @SerializedName("started_at")
  String startedAt = "";

  @SerializedName("completed_at")
  String completedAt = "";

  @SerializedName("stellar_transaction_id")
  String stellarTransactionId = "";

  @SerializedName("external_transaction_id")
  String externalTransactionId;

  String message;

  Boolean refunded = false;
  Refunds refunds;

  String from = "";

  String to = "";

  @SerializedName("account_memo")
  String accountMemo;

  @SerializedName("muxed_account")
  String muxedAccount;
}

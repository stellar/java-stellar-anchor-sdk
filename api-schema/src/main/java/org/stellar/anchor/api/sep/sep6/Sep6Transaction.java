package org.stellar.anchor.api.sep.sep6;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import org.stellar.anchor.api.shared.InstructionField;
import org.stellar.anchor.api.shared.Refunds;

@Data
@Builder
public class Sep6Transaction {
  String id;

  String kind;

  String status;

  @SerializedName("status_eta")
  Long statusEta;

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

  @SerializedName("quote_id")
  String quoteId;

  String from;

  String to;

  String depositMemo;

  String depositMemoType;

  String withdrawMemoAccount;

  String withdrawMemo;

  String withdrawMemoType;

  @SerializedName("started_at")
  String startedAt;

  @SerializedName("updated_at")
  String updatedAt;

  @SerializedName("completed_at")
  String completedAt;

  @SerializedName("stellar_transaction_id")
  String stellarTransactionId;

  @SerializedName("external_transaction_id")
  String externalTransactionId;

  String message;

  Refunds refunds;

  @SerializedName("required_info_message")
  String requiredInfoMessage;

  @SerializedName("required_info_updates")
  List<String> requiredInfoUpdates;

  @SerializedName("required_customer_info_message")
  String requiredCustomerInfoMessage;

  @SerializedName("required_customer_info_updates")
  List<String> requiredCustomerInfoUpdates;

  Map<String, InstructionField> instructions;
}

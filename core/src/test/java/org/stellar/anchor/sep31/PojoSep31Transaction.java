package org.stellar.anchor.sep31;

import com.google.gson.annotations.SerializedName;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import lombok.Data;
import org.stellar.anchor.api.sep.AssetInfo;
import org.stellar.anchor.event.models.StellarTransaction;

@Data
public class PojoSep31Transaction implements Sep31Transaction {
  String id;
  String status;

  @SerializedName("status_eta")
  Long statusEta;

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

  @SerializedName("stellar_account_id")
  String stellarAccountId;

  @SerializedName("stellar_memo")
  String stellarMemo;

  @SerializedName("stellar_memo_type")
  String stellarMemoType;

  @SerializedName("started_at")
  Instant startedAt;

  @SerializedName("completed_at")
  Instant completedAt;

  @SerializedName("stellar_transaction_id")
  String stellarTransactionId;

  @SerializedName("external_transaction_id")
  String externalTransactionId;

  @SerializedName("required_info_message")
  String requiredInfoMessage;

  @SerializedName("quote_id")
  String quoteId;

  @SerializedName("client_domain")
  String clientDomain;

  @SerializedName("required_info_updates")
  AssetInfo.Sep31TxnFieldSpecs requiredInfoUpdates;

  Map<String, String> fields;
  Boolean refunded;
  Refunds refunds;

  @SerializedName("updated_at")
  Instant updatedAt;

  @SerializedName("transfer_received_at")
  Instant transferReceivedAt;

  String message;

  @SerializedName("amount_expected")
  String amountExpected;

  @SerializedName("stellar_transactions")
  Set<StellarTransaction> stellarTransactions = new java.util.LinkedHashSet<>();
}

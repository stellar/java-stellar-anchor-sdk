package org.stellar.anchor.platform.paymentobserver;

import com.google.gson.annotations.SerializedName;
import java.time.Instant;
import lombok.Builder;
import lombok.Data;
import org.stellar.platform.apis.shared.Amount;

@Data
@Builder
public class TransactionEvent {
  @SerializedName("event_id")
  String event_id;

  String type;

  Integer sep; // TODO ENUM

  String kind;

  String status; // TODO ENUM

  @SerializedName("transaction_id")
  String transactionId;

  @SerializedName("amount_expected")
  Amount amountExpected;

  @SerializedName("amount_in")
  Amount amountIn;

  @SerializedName("amount_out")
  Amount amountOut;

  @SerializedName("amount_fee")
  Amount amountFee;

  @SerializedName("quote_id")
  String quoteId;

  @SerializedName("started_at")
  Instant startedAt;

  @SerializedName("updated_at")
  Instant updatedAt;

  @SerializedName("completed_at")
  Instant completedAt;

  @SerializedName("transfer_received_at")
  Instant transferReceivedAt;

  String message;

  @SerializedName("stellar_transactions")
  StellarTransaction stellarTransactions; // TODO should be array

  @SerializedName("external_transaction_id")
  String externalTransactionId;

  @SerializedName("custodial_transaction_id")
  String custodialTransactionId;

  @SerializedName("source_account")
  String sourceAccount;

  @SerializedName("destination_account")
  String destinationAccount;

  @SerializedName("client_domain")
  String clientDomain;
}

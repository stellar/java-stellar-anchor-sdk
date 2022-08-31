package org.stellar.anchor.api.platform;

import com.google.gson.annotations.SerializedName;
import java.time.Instant;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.stellar.anchor.api.shared.*;

@Data
@SuperBuilder
@NoArgsConstructor
public class GetTransactionResponse {
  String id;
  Integer sep;
  String kind;
  String status;

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
  Refund refunds;

  @SerializedName("stellar_transactions")
  List<StellarTransaction> stellarTransactions;

  @SerializedName("external_transaction_id")
  String externalTransactionId;

  @SerializedName("custodial_transaction_id")
  String custodialTransactionId;

  Customers customers;
  StellarId creator;
}

package org.stellar.platform.apis.shared;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.List;

@Data
@SuperBuilder
@NoArgsConstructor
public class Transaction {
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
  Refunds refunds;

  @SerializedName("stellar_transactions")
  List<StellarTransaction> stellarTransactions;

  @SerializedName("external_id")
  String externalId;

  @SerializedName("custodial_id")
  String custodialId;

  Customers customers;
  StellarId creator;
}

@Data
class StellarTransaction {
  String id;
  String memo;

  @SerializedName("memo_type")
  String memoType;

  @SerializedName("created_at")
  Instant createdAt;

  String envelope;
  Payment payment;
}

@Data
class Payment {
  @SerializedName("operation_id")
  String operationId;

  @SerializedName("source_account")
  String sourceAccount;

  @SerializedName("destination_account")
  String destinationAccount;

  Amount amount;
}

@Data
class Customers {
  StellarId sender;
  StellarId receiver;
}

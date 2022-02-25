package org.stellar.platform.apis.shared;

import com.google.gson.annotations.SerializedName;
import java.time.LocalDateTime;
import java.util.List;

import lombok.Data;

@Data
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
  LocalDateTime startedAt;

  @SerializedName("updated_at")
  LocalDateTime updatedAt;

  @SerializedName("completed_at")
  LocalDateTime completedAt;

  @SerializedName("transfer_received_at")
  LocalDateTime transferReceivedAt;

  String message;
  Refund refund;

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
  LocalDateTime createdAt;

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

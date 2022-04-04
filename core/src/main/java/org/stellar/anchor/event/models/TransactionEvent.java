package org.stellar.anchor.event.models;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TransactionEvent implements AnchorEvent {
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
  StellarTransaction stellarTransactions; // TODO should be array

  @SerializedName("external_transaction_id")
  String externalTransactionId;

  @SerializedName("custodial_transaction_id")
  String custodialTransactionId;

  @SerializedName("source_account")
  String sourceAccount;

  @SerializedName("destination_account")
  String destinationAccount;

  Customers customers;

  @SerializedName("client_domain")
  String clientDomain;

  public TransactionEvent() {}

  public TransactionEvent(
      String event_id,
      String type,
      Integer sep,
      String kind,
      String status,
      String transactionId,
      Amount amountExpected,
      Amount amountIn,
      Amount amountOut,
      Amount amountFee,
      String quoteId,
      LocalDateTime startedAt,
      LocalDateTime updatedAt,
      LocalDateTime completedAt,
      LocalDateTime transferReceivedAt,
      String message,
      Refund refund,
      StellarTransaction stellarTransactions,
      String externalTransactionId,
      String custodialTransactionId,
      String sourceAccount,
      String destinationAccount,
      Customers customers,
      String clientDomain) {
    this.event_id = event_id;
    this.type = type;
    this.sep = sep;
    this.kind = kind;
    this.status = status;
    this.transactionId = transactionId;
    this.amountExpected = amountExpected;
    this.amountIn = amountIn;
    this.amountOut = amountOut;
    this.amountFee = amountFee;
    this.quoteId = quoteId;
    this.startedAt = startedAt;
    this.updatedAt = updatedAt;
    this.completedAt = completedAt;
    this.transferReceivedAt = transferReceivedAt;
    this.message = message;
    this.refund = refund;
    this.stellarTransactions = stellarTransactions;
    this.externalTransactionId = externalTransactionId;
    this.custodialTransactionId = custodialTransactionId;
    this.sourceAccount = sourceAccount;
    this.destinationAccount = destinationAccount;
    this.customers = customers;
    this.clientDomain = clientDomain;
  }
}

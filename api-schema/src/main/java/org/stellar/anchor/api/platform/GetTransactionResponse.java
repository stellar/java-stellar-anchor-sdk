package org.stellar.anchor.api.platform;

import com.google.gson.annotations.SerializedName;
import java.time.Instant;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.stellar.anchor.api.shared.Amount;

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
  Refunds refunds;

  @SerializedName("stellar_transactions")
  List<StellarTransaction> stellarTransactions;

  @SerializedName("external_id")
  String externalId;

  @SerializedName("custodial_id")
  String custodialId;

  Customers customers;
  StellarId creator;

  @Data
  public static class StellarTransaction {
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
  public static class Payment {
    @SerializedName("operation_id")
    String operationId;

    @SerializedName("source_account")
    String sourceAccount;

    @SerializedName("destination_account")
    String destinationAccount;

    Amount amount;
  }

  @Data
  public static class Customers {
    StellarId sender;
    StellarId receiver;
  }

  @Data
  public static class StellarId {
    String id;
    String account;
    String memo;

    @SerializedName("memo_type")
    String memoType;
  }
}

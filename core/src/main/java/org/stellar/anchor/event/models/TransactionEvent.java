package org.stellar.anchor.event.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.google.gson.annotations.SerializedName;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.stellar.anchor.api.shared.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TransactionEvent implements AnchorEvent {
  @JsonProperty("event_id")
  @SerializedName("event_id")
  String eventId;

  Type type;

  public String getType() {
    return this.type.type;
  }

  String id;

  Sep sep;

  Kind kind;

  Status status;

  @JsonProperty("status_change")
  @SerializedName("status_change")
  StatusChange statusChange;

  @JsonProperty("amount_expected")
  @SerializedName("amount_expected")
  Amount amountExpected;

  @JsonProperty("amount_in")
  @SerializedName("amount_in")
  Amount amountIn;

  @JsonProperty("amount_out")
  @SerializedName("amount_out")
  Amount amountOut;

  @JsonProperty("amount_fee")
  @SerializedName("amount_fee")
  Amount amountFee;

  @JsonProperty("quote_id")
  @SerializedName("quote_id")
  String quoteId;

  @JsonProperty("started_at")
  @SerializedName("started_at")
  Instant startedAt;

  @JsonProperty("updated_at")
  @SerializedName("updated_at")
  Instant updatedAt;

  @JsonProperty("completed_at")
  @SerializedName("completed_at")
  Instant completedAt;

  @JsonProperty("transfer_received_at")
  @SerializedName("transfer_received_at")
  Instant transferReceivedAt;

  String message;

  Refunds refunds;

  @JsonProperty("stellar_transactions")
  @SerializedName("stellar_transactions")
  List<StellarTransaction> stellarTransactions;

  @JsonProperty("external_transaction_id")
  @SerializedName("external_transaction_id")
  String externalTransactionId;

  @JsonProperty("custodial_transaction_id")
  @SerializedName("custodial_transaction_id")
  String custodialTransactionId;

  @JsonProperty("source_account")
  @SerializedName("source_account")
  String sourceAccount;

  @JsonProperty("destination_account")
  @SerializedName("destination_account")
  String destinationAccount;

  Customers customers;

  StellarId creator;

  public enum Status {
    INCOMPLETE("incomplete"),
    PENDING_SENDER("pending_sender"),
    PENDING_STELLAR("pending_stellar"),
    PENDING_CUSTOMER_INFO_UPDATE("pending_customer_info_update"),
    PENDING_RECEIVER("pending_receiver"),
    PENDING_EXTERNAL("pending_external"),
    COMPLETED("completed"),
    EXPIRED("expired"),
    ERROR("error");

    @JsonValue public final String status;

    Status(String status) {
      this.status = status;
    }

    @Override
    public String toString() {
      return this.status;
    }

    public static Status from(String statusStr) {
      for (Status status : values()) {
        if (Objects.equals(status.status, statusStr)) {
          return status;
        }
      }
      throw new IllegalArgumentException("No matching constant for [" + statusStr + "]");
    }
  }

  @Data
  @AllArgsConstructor
  public static class StatusChange {
    Status from;
    Status to;

    StatusChange() {}
  }

  public enum Sep {
    @SuppressWarnings("unused")
    SEP_24(24),
    SEP_31(31);

    private final Integer sep;

    Sep(Integer sep) {
      this.sep = sep;
    }

    @JsonValue
    public Integer getSep() {
      return sep;
    }
  }

  public enum Type {
    TRANSACTION_CREATED("transaction_created"),
    TRANSACTION_STATUS_CHANGED("transaction_status_changed"),
    @SuppressWarnings("unused")
    TRANSACTION_ERROR("transaction_error");

    @JsonValue public final String type;

    Type(String type) {
      this.type = type;
    }
  }

  public enum Kind {
    @SuppressWarnings("unused")
    UNDEFINED("undefined"),
    RECEIVE("receive"),
    DEPOSIT("deposit"),
    WITHDRAW("withdrawal");

    public final String kind;

    Kind(String kind) {
      this.kind = kind;
    }

    @JsonValue
    public String getKind() {
      return kind;
    }

    public static Kind from(String str) {
      for (Kind kind : values()) {
        if (kind.kind.equals(str)) {
          return kind;
        }
      }
      throw new IllegalArgumentException("No matching constant for [" + str + "]");
    }
  }
}

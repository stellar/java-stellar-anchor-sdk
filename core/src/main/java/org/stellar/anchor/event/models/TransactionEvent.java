package org.stellar.anchor.event.models;

import com.fasterxml.jackson.annotation.JsonValue;
import com.google.gson.annotations.SerializedName;
import java.time.Instant;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.stellar.anchor.api.shared.Amount;

@Data
@Builder
@AllArgsConstructor
public class TransactionEvent implements AnchorEvent {
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

  @SerializedName("status_change")
  StatusChange statusChange;

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

  Refund refund;

  @SerializedName("stellar_transactions")
  StellarTransaction[] stellarTransactions;

  @SerializedName("external_transaction_id")
  String externalTransactionId;

  @SerializedName("custodial_transaction_id")
  String custodialTransactionId;

  @SerializedName("source_account")
  String sourceAccount;

  @SerializedName("destination_account")
  String destinationAccount;

  Customers customers;

  StellarId creator;

  public enum Status {
    PENDING_SENDER("pending_sender"),
    PENDING_STELLAR("pending_stellar"),
    PENDING_CUSTOMER_INFO_UPDATE("pending_customer_info_update"),
    PENDING_TRANSACTION_INFO_UPDATE("pending_transaction_info_update"),
    PENDING_RECEIVER("pending_receiver"),
    PENDING_EXTERNAL("pending_external"),
    COMPLETED("completed"),
    ERROR("error");

    @JsonValue public final String status;

    Status(String status) {
      this.status = status;
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
  @Builder
  @AllArgsConstructor
  public static class StatusChange {
    Status from;
    Status to;

    StatusChange() {}
  }

  public enum Sep {
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
    TRANSACTION_ERROR("transaction_error");

    @JsonValue public final String type;

    Type(String type) {
      this.type = type;
    }
  }

  public enum Kind {
    RECEIVE("receive");

    public final String kind;

    Kind(String kind) {
      this.kind = kind;
    }

    @JsonValue
    public String getKind() {
      return kind;
    }
  }

  public TransactionEvent() {}
}

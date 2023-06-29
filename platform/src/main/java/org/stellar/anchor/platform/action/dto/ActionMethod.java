package org.stellar.anchor.platform.action.dto;

import com.google.gson.annotations.SerializedName;
import java.util.Objects;

public enum ActionMethod {
  @SerializedName("notify_interactive_flow_completed")
  NOTIFY_INTERACTIVE_FLOW_COMPLETED("notify_interactive_flow_completed"),

  @SerializedName("request_offchain_funds")
  REQUEST_OFFCHAIN_FUNDS("request_offchain_funds"),

  @SerializedName("notify_offchain_funds_sent")
  NOTIFY_OFFCHAIN_FUNDS_SENT("notify_offchain_funds_sent"),

  @SerializedName("notify_offchain_funds_received")
  NOTIFY_OFFCHAIN_FUNDS_RECEIVED("notify_offchain_funds_received"),

  @SerializedName("notify_refund_initiated")
  NOTIFY_REFUND_INITIATED("notify_refund_initiated"),

  @SerializedName("notify_refund_sent")
  NOTIFY_REFUND_SENT("notify_refund_sent"),

  @SerializedName("request_trust")
  REQUEST_TRUST("request_trust"),

  @SerializedName("notify_trust_set")
  NOTIFY_TRUST_SET("notify_trust_set"),

  @SerializedName("do_stellar_payment")
  DO_STELLAR_PAYMENT("do_stellar_payment"),

  @SerializedName("notify_onchain_funds_sent")
  NOTIFY_ONCHAIN_FUNDS_SENT("notify_onchain_funds_sent"),

  @SerializedName("notify_onchain_funds_received")
  NOTIFY_ONCHAIN_FUNDS_RECEIVED("notify_onchain_funds_received"),

  @SerializedName("request_offchain_funds_collected")
  REQUEST_OFFCHAIN_FUNDS_COLLECTED("request_offchain_funds_collected"),

  @SerializedName("notify_transaction_error")
  NOTIFY_TRANSACTION_ERROR("notify_transaction_error"),

  @SerializedName("notify_transaction_expired")
  NOTIFY_TRANSACTION_EXPIRED("notify_transaction_expired"),

  @SerializedName("do_stellar_refund")
  DO_STELLAR_REFUND("do_stellar_refund"),

  @SerializedName("request_onchain_funds")
  REQUEST_ONCHAIN_FUNDS("request_onchain_funds");

  private final String method;

  ActionMethod(String method) {
    this.method = method;
  }

  public static ActionMethod from(String method) {
    for (ActionMethod sts : values()) {
      if (Objects.equals(sts.method, method)) {
        return sts;
      }
    }
    throw new IllegalArgumentException(String.format("No matching action method[%s]", method));
  }
}

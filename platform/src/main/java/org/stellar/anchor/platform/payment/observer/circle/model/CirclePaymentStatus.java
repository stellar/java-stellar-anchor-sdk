package org.stellar.anchor.platform.payment.observer.circle.model;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import org.stellar.anchor.platform.payment.common.Payment;

@Getter
public enum CirclePaymentStatus {
  @SerializedName("pending")
  PENDING("pending"),

  /**
   * Confirmed means almost completed in Circle incoming payments.
   *
   * @link https://developers.circle.com/docs/circle-api-resources#payment-attributes
   */
  @SerializedName("confirmed")
  CONFIRMED("confirmed"),

  @SerializedName("failed")
  FAILED("failed"),

  /**
   * Paid means successful in Circle incoming payments.
   *
   * @link https://developers.circle.com/docs/circle-api-resources#payment-attributes
   */
  @SerializedName("paid")
  PAID("paid"),

  @SerializedName("complete")
  COMPLETE("complete");

  private final String name;

  CirclePaymentStatus(String name) {
    this.name = name;
  }

  @Override
  public String toString() {
    return name;
  }

  public Payment.Status toPaymentStatus() {
    switch (this) {
      case PENDING:
      case CONFIRMED:
        return Payment.Status.PENDING;
      case COMPLETE:
      case PAID:
        return Payment.Status.SUCCESSFUL;
      case FAILED:
        return Payment.Status.FAILED;
      default:
        throw new RuntimeException("unsupported CirclePaymentStatus -> Payment.Status conversion");
    }
  }
}

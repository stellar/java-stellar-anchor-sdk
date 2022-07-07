package org.stellar.anchor.event.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.stellar.anchor.api.shared.Amount;

@Data
@Builder
@AllArgsConstructor
public class Refund {

  @JsonProperty("amount_refunded")
  @SerializedName("amount_refunded")
  Amount amountRefunded;

  @JsonProperty("amount_fee")
  @SerializedName("amount_fee")
  Amount amountFee;

  RefundPayment[] payments;

  public Refund() {}
}

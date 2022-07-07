package org.stellar.anchor.event.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.stellar.anchor.api.shared.Amount;

@Data
@Builder
@AllArgsConstructor
public class Refund {
  String type;
  Amount amount;

  @JsonProperty("requested_at")
  @SerializedName("requested_at")
  Instant requestedAt;

  @JsonProperty("refunded_at")
  @SerializedName("refunded_at")
  Instant refundedAt;

  public Refund() {}
}

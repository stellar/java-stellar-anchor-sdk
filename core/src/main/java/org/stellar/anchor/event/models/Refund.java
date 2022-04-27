package org.stellar.anchor.event.models;

import com.google.gson.annotations.SerializedName;
import java.time.LocalDateTime;
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

  @SerializedName("requested_at")
  LocalDateTime requestedAt;

  @SerializedName("refunded_at")
  LocalDateTime refundedAt;

  public Refund() {}
}

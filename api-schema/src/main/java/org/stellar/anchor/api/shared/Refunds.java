package org.stellar.anchor.api.shared;

import com.google.gson.annotations.SerializedName;
import java.time.Instant;
import lombok.Data;

@Data
public class Refunds {
  String type;
  Amount amount;

  @SerializedName("requested_at")
  Instant requestedAt;

  @SerializedName("refunded_at")
  Instant refundedAt;
}

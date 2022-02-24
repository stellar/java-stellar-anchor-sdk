package org.stellar.platform.apis.shared;

import com.google.gson.annotations.SerializedName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class Refund {
  String type;
  Amount amount;

  @SerializedName("requested_at")
  LocalDateTime requestedAt;

  @SerializedName("refunded_at")
  LocalDateTime refundedAt;
}

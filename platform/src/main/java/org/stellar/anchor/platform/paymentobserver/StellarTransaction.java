package org.stellar.anchor.platform.paymentobserver;

import com.google.gson.annotations.SerializedName;
import java.time.Instant;
import lombok.Builder;
import lombok.Data;
import org.stellar.anchor.paymentservice.Payment;

@Data
@Builder
public class StellarTransaction {
  String id;
  String memo;

  @SerializedName("memo_type")
  String memoType;

  @SerializedName("created_at")
  Instant createdAt;

  String envelope;
  Payment payment;
}

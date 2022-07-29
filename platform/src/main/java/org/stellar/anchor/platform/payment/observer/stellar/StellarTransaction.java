package org.stellar.anchor.platform.payment.observer.stellar;

import com.google.gson.annotations.SerializedName;
import java.time.Instant;
import lombok.Builder;
import lombok.Data;

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
  StellarPayment payment;
}

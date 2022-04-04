package org.stellar.anchor.event.models;

import com.google.gson.annotations.SerializedName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
class StellarTransaction {
  String id;
  String memo;

  @SerializedName("memo_type")
  String memoType;

  @SerializedName("created_at")
  LocalDateTime createdAt;

  String envelope;
  Payment payment;
}

@Data
class Customers {
  StellarId sender;
  StellarId receiver;
}

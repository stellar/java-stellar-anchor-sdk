package org.stellar.anchor.event.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class StellarTransaction {
  String id;
  String memo;

  @JsonProperty("memo_type")
  @SerializedName("memo_type")
  String memoType;

  @JsonProperty("created_at")
  @SerializedName("created_at")
  Instant createdAt;

  String envelope;
  Payment[] payments;

  public StellarTransaction() {}
}

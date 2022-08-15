package org.stellar.anchor.api.shared;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class RefundPayment {
  String id;

  @JsonProperty("id_type")
  @SerializedName("id_type")
  IdType idType;

  Amount amount;

  Amount fee;

  @JsonProperty("requested_at")
  @SerializedName("requested_at")
  Instant requestedAt;

  @JsonProperty("refunded_at")
  @SerializedName("refunded_at")
  Instant refundedAt;

  public RefundPayment() {}

  public enum IdType {
    @SerializedName("stellar")
    STELLAR("stellar"),

    @SerializedName("external")
    EXTERNAL("external");

    private final String name;

    IdType(String name) {
      this.name = name;
    }

    @Override
    public String toString() {
      return name;
    }
  }
}

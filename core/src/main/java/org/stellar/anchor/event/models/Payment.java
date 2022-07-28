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
public class Payment {
  @JsonProperty("operation_id")
  @SerializedName("operation_id")
  String operationId;

  @JsonProperty("source_account")
  @SerializedName("source_account")
  String sourceAccount;

  @JsonProperty("destination_account")
  @SerializedName("destination_account")
  String destinationAccount;

  Amount amount;

  public Payment() {}
}

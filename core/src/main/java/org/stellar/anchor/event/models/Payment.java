package org.stellar.anchor.event.models;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
class Payment {
  @SerializedName("operation_id")
  String operationId;

  @SerializedName("source_account")
  String sourceAccount;

  @SerializedName("destination_account")
  String destinationAccount;

  Amount amount;
}

package org.stellar.anchor.api.sep.sep24;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Data;

/** The request body of the GET /transaction endpoint of SEP-24. */
@AllArgsConstructor
@Data
public class GetTransactionRequest {
  String id;

  @SerializedName("stellar_transaction_id")
  String stellarTransactionId;

  @SerializedName("external_transaction_id")
  String externalTransactionId;

  String lang;
}

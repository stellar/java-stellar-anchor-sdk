package org.stellar.anchor.platform.payment.observer.stellar;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import org.stellar.anchor.api.shared.Amount;

@Builder
public class StellarPayment {
  @SerializedName("operation_id")
  String operationId;

  @SerializedName("source_account")
  String sourceAccount;

  @SerializedName("destination_account")
  String destinationAccount;

  Amount amount;
}

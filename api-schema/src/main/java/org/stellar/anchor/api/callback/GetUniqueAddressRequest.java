package org.stellar.anchor.api.callback;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class GetUniqueAddressRequest {
  @SerializedName("transaction_id")
  String transactionId;
}

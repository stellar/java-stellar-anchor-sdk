package org.stellar.anchor.api.callback;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GetUniqueAddressRequest {
  @SerializedName("transaction_id")
  String transactionId;
}

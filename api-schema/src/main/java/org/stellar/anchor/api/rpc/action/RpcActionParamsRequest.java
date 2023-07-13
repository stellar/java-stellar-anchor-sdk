package org.stellar.anchor.api.rpc.action;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class RpcActionParamsRequest {

  @SerializedName("transaction_id")
  private String transactionId;

  private String message;
}

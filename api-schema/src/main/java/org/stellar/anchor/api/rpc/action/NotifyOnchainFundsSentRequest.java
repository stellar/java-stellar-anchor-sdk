package org.stellar.anchor.api.rpc.action;

import com.google.gson.annotations.SerializedName;
import javax.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class NotifyOnchainFundsSentRequest extends RpcActionParamsRequest {

  @SerializedName("stellar_transaction_id")
  @NotBlank
  private String stellarTransactionId;
}

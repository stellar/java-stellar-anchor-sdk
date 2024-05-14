package org.stellar.anchor.api.rpc.method;

import com.google.gson.annotations.SerializedName;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class NotifyOffchainFundsAvailableRequest extends RpcMethodParamsRequest {

  @SerializedName("external_transaction_id")
  private String externalTransactionId;

  @SerializedName("user_action_required_by")
  Instant userActionRequiredBy;
}

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
public class NotifyOffchainFundsSentRequest extends RpcMethodParamsRequest {

  @SerializedName("funds_sent_at")
  private Instant fundsSentAt;

  @SerializedName("external_transaction_id")
  private String externalTransactionId;
}

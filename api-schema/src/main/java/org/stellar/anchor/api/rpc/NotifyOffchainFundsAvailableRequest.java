package org.stellar.anchor.api.rpc;

import java.time.Instant;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class NotifyOffchainFundsAvailableRequest extends RpcParamsRequest {

  private Instant fundsReceivedAt;
  private String externalTransactionId;
}

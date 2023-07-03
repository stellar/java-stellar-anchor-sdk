package org.stellar.anchor.api.rpc.action;

import java.time.Instant;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class NotifyOffchainFundsAvailableRequest extends RpcActionParamsRequest {

  private Instant fundsReceivedAt;
  private String externalTransactionId;
}

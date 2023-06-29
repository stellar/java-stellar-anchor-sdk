package org.stellar.anchor.platform.action.dto;

import java.time.Instant;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class RequestOffchainFundsCollectedRequest extends RpcParamsRequest {

  private Instant fundsReceivedAt;
  private String externalTransactionId;
}

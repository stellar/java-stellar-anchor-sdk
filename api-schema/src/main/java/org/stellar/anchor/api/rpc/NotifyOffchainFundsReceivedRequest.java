package org.stellar.anchor.api.rpc;

import java.time.Instant;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class NotifyOffchainFundsReceivedRequest extends RpcParamsRequest {

  private Instant fundsReceivedAt;
  private String externalTransactionId;
  private AmountRequest amountIn;
  private AmountRequest amountOut;
  private AmountRequest amountFee;
}

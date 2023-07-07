package org.stellar.anchor.api.rpc.action;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class NotifyOffchainFundsReceivedRequest extends RpcActionParamsRequest {

  private Instant fundsReceivedAt;
  private String externalTransactionId;
  private String amountIn;
  private String amountOut;
  private String amountFee;
}

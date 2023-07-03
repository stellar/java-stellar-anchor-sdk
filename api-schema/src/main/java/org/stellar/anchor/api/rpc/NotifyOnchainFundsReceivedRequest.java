package org.stellar.anchor.api.rpc;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class NotifyOnchainFundsReceivedRequest extends RpcParamsRequest {

  private String stellarTransactionId;
  private AmountRequest amountIn;
  private AmountRequest amountOut;
  private AmountRequest amountFee;
}

package org.stellar.anchor.api.rpc;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class RequestOffchainFundsRequest extends RpcParamsRequest {

  private AmountRequest amountIn;
  private AmountRequest amountOut;
  private AmountRequest amountFee;
  private AmountRequest amountExpected;
}

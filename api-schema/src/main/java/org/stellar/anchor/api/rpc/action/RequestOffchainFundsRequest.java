package org.stellar.anchor.api.rpc.action;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = false)
public class RequestOffchainFundsRequest extends RpcActionParamsRequest {

  private AmountRequest amountIn;
  private AmountRequest amountOut;
  private AmountRequest amountFee;
  private String amountExpected;
}

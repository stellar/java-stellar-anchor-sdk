package org.stellar.anchor.api.rpc.action;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class RequestOnchainFundsRequest extends RpcActionParamsRequest {

  private AmountRequest amountIn;
  private AmountRequest amountOut;
  private AmountRequest amountFee;
  private String amountExpected;
  private String destinationAccount;
  private String memo;
  private String memoType;
}

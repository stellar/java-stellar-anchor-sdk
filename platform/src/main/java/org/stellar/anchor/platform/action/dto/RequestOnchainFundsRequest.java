package org.stellar.anchor.platform.action.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class RequestOnchainFundsRequest extends RpcParamsRequest {

  private AmountRequest amountIn;
  private AmountRequest amountOut;
  private AmountRequest amountFee;
  private AmountRequest amountExpected;
  private String destinationAccount;
  private String memo;
  private String memoType;
}

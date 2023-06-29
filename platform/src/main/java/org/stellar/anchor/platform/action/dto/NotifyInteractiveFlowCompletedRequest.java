package org.stellar.anchor.platform.action.dto;

import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class NotifyInteractiveFlowCompletedRequest extends RpcParamsRequest {

  @NotNull private AmountRequest amountIn;

  @NotNull private AmountRequest amountOut;

  @NotNull private AmountRequest amountFee;

  private AmountRequest amountExpected;
}

package org.stellar.anchor.api.rpc.action;

import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class NotifyInteractiveFlowCompletedRequest extends RpcActionParamsRequest {

  @NotNull private AmountRequest amountIn;

  @NotNull private AmountRequest amountOut;

  @NotNull private AmountRequest amountFee;

  private AmountRequest amountExpected;
}

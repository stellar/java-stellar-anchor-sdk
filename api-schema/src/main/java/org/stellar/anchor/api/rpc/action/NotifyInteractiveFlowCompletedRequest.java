package org.stellar.anchor.api.rpc.action;

import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class NotifyInteractiveFlowCompletedRequest extends RpcActionParamsRequest {

  @NotNull private AmountAssetRequest amountIn;

  @NotNull private AmountAssetRequest amountOut;

  @NotNull private AmountAssetRequest amountFee;

  private AmountRequest amountExpected;
}

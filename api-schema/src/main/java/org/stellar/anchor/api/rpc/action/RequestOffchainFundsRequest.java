package org.stellar.anchor.api.rpc.action;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class RequestOffchainFundsRequest extends RpcActionParamsRequest {

  private AmountAssetRequest amountIn;
  private AmountAssetRequest amountOut;
  private AmountAssetRequest amountFee;
  private AmountRequest amountExpected;
}

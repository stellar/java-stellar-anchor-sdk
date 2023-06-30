package org.stellar.anchor.platform.action.dto;

import javax.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class NotifyOnchainFundsReceivedRequest extends RpcParamsRequest {

  @NotBlank private String stellarTransactionId;
  private AmountRequest amountIn;
  private AmountRequest amountOut;
  private AmountRequest amountFee;
}

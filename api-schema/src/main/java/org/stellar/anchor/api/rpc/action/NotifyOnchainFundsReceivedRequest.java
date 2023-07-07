package org.stellar.anchor.api.rpc.action;

import javax.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class NotifyOnchainFundsReceivedRequest extends RpcActionParamsRequest {

  @NotBlank private String stellarTransactionId;
  private String amountIn;
  private String amountOut;
  private String amountFee;
}

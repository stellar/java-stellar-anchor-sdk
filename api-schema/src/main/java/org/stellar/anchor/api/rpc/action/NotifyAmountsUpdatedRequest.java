package org.stellar.anchor.api.rpc.action;

import javax.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class NotifyAmountsUpdatedRequest extends RpcActionParamsRequest {

  @NotBlank private String amountOut;

  @NotBlank private String amountFee;
}

package org.stellar.anchor.platform.action.dto;

import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class DoStellarRefundRequest extends RpcParamsRequest {

  @NotNull private Refund refund;

  @Data
  public static class Refund {

    @NotNull private AmountRequest amount;

    @NotNull private AmountRequest amountFee;
  }
}

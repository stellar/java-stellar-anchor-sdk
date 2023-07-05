package org.stellar.anchor.api.rpc.action;

import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class DoStellarRefundRequest extends RpcActionParamsRequest {

  @NotNull private Refund refund;
  private String memo;
  private String memoType;

  @Data
  public static class Refund {

    @NotNull private AmountRequest amount;

    @NotNull private AmountRequest amountFee;
  }
}

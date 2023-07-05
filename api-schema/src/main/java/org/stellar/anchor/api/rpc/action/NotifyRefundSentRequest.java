package org.stellar.anchor.api.rpc.action;

import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class NotifyRefundSentRequest extends RpcActionParamsRequest {

  private Refund refund;

  @Data
  public static class Refund {
    @NotNull private String id;
    @NotNull private String amount;
    @NotNull private String amountFee;
  }
}

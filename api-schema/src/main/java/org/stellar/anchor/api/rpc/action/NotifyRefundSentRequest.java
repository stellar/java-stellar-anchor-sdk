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
public class NotifyRefundSentRequest extends RpcActionParamsRequest {

  private Refund refund;

  @Data
  public static class Refund {

    @NotNull private String id;
    @NotNull private String amount;
    @NotNull private String amountFee;
  }
}

package org.stellar.anchor.api.rpc;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class NotifyRefundSentRequest extends RpcParamsRequest {

  private String id;
  private AmountRequest amount;
  private AmountRequest amountFee;
}

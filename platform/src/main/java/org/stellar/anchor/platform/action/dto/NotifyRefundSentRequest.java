package org.stellar.anchor.platform.action.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class NotifyRefundSentRequest extends RpcParamsRequest {

  private String id;
  private AmountRequest amount;
  private AmountRequest amountFee;
}

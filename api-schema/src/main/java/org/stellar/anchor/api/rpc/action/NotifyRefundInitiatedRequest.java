package org.stellar.anchor.api.rpc.action;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class NotifyRefundInitiatedRequest extends RpcActionParamsRequest {

  private String id;
  private AmountRequest amount;
  private AmountRequest amountFee;
}

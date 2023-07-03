package org.stellar.anchor.api.rpc;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class NotifyRefundInitiatedRequest extends RpcParamsRequest {

  private String id;
  private AmountRequest amount;
  private AmountRequest amountFee;
}

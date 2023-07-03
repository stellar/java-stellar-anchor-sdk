package org.stellar.anchor.api.rpc;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class NotifyTransactionErrorRequest extends RpcParamsRequest {

  private String id;
  private AmountRequest amount;
  private AmountRequest amountFee;
  private boolean completed;
}

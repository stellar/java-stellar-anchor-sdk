package org.stellar.anchor.api.rpc.action;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class NotifyTransactionErrorRequest extends RpcActionParamsRequest {

  private String id;
  private AmountRequest amount;
  private AmountRequest amountFee;
  private boolean completed;
}

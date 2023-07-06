package org.stellar.anchor.api.rpc.action;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = false)
public class NotifyTransactionExpiredRequest extends RpcActionParamsRequest {

  private String id;
  private AmountRequest amount;
  private AmountRequest amountFee;
  private boolean completed;
}

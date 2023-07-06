package org.stellar.anchor.api.rpc.action;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = false)
public class NotifyOffchainFundsAvailableRequest extends RpcActionParamsRequest {

  private String externalTransactionId;
}

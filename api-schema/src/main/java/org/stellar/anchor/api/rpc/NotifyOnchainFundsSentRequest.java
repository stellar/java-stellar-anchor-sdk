package org.stellar.anchor.api.rpc;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class NotifyOnchainFundsSentRequest extends RpcParamsRequest {

  private String stellarTransactionId;
}

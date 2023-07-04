package org.stellar.anchor.api.rpc.action;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
public class RpcActionParamsRequest {

  private String transactionId;
  private String message;
}

package org.stellar.anchor.api.rpc.action;

import lombok.Data;

@Data
public class RpcActionParamsRequest {

  private String transactionId;
  private String message;
}

package org.stellar.anchor.api.rpc;

import lombok.Data;

@Data
public class RpcParamsRequest {

  private String transactionId;
  private String message;
}

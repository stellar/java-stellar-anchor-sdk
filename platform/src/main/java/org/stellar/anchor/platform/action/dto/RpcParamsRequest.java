package org.stellar.anchor.platform.action.dto;

import lombok.Data;

@Data
public class RpcParamsRequest {

  private String transactionId;
  private String message;
}

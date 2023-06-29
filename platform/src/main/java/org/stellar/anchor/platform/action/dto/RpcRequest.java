package org.stellar.anchor.platform.action.dto;

import lombok.Data;

@Data
public class RpcRequest {

  private String id;
  private String jsonrpc;
  private ActionMethod method;
  private RpcParamsRequest params;
}

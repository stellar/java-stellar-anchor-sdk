package org.stellar.anchor.api.rpc;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RpcRequest {
  private Object id;
  private String jsonrpc;
  private String method;
  private Object params;
}

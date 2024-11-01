package org.stellar.anchor.api.rpc;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RpcRequest {
  // ID of the request
  private Object id;
  // Version of the JSON-RPC protocol
  private String jsonrpc;
  // Name of the method to be invoked
  private String method;
  // Parameters to pass to the method
  private Object params;
}

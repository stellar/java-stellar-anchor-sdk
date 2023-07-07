package org.stellar.anchor.api.rpc;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RpcResponse {
  private String jsonrpc;
  private Object result;
  private RpcError error;
  private Object id;

  @Data
  @Builder
  public static class RpcError {
    private int code;
    private String message;
    private Object data;
  }
}

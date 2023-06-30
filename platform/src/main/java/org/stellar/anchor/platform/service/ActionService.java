package org.stellar.anchor.platform.service;

import static org.stellar.anchor.platform.utils.RpcUtil.getRpcErrorResponse;
import static org.stellar.anchor.platform.utils.RpcUtil.getRpcSuccessResponse;
import static org.stellar.anchor.platform.utils.RpcUtil.validateRpcRequest;
import static org.stellar.anchor.util.Log.infoF;

import java.util.List;
import java.util.stream.Collectors;
import org.stellar.anchor.api.exception.RpcException;
import org.stellar.anchor.api.exception.rpc.InternalErrorException;
import org.stellar.anchor.api.rpc.RpcRequest;
import org.stellar.anchor.api.rpc.RpcResponse;

public class ActionService {

  public List<RpcResponse> handleRpcCalls(List<RpcRequest> rpcCalls) {
    return rpcCalls.stream()
        .map(
            rpcCall -> {
              final Object rpcId = rpcCall.getId();
              try {
                validateRpcRequest(rpcCall);
                return getRpcSuccessResponse(rpcId, processRpcCall(rpcCall));
              } catch (RpcException ex) {
                return getRpcErrorResponse(rpcId, ex);
              } catch (Exception ex) {
                return getRpcErrorResponse(rpcId, new InternalErrorException(ex.getMessage()));
              }
            })
        .collect(Collectors.toList());
  }

  private Object processRpcCall(RpcRequest rpcCall) throws RpcException {
    infoF("Started processing of RPC call with method [{}]", rpcCall.getMethod());
    // Add logic to get handle by method name and process rpc call
    return null;
  }
}

package org.stellar.anchor.platform.service;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.stellar.anchor.platform.utils.RpcUtil.getRpcErrorResponse;
import static org.stellar.anchor.platform.utils.RpcUtil.getRpcSuccessResponse;
import static org.stellar.anchor.platform.utils.RpcUtil.validateRpcRequest;
import static org.stellar.anchor.util.Log.debugF;

import java.util.List;
import java.util.Map;
import org.stellar.anchor.api.exception.AnchorException;
import org.stellar.anchor.api.exception.RpcException;
import org.stellar.anchor.api.exception.rpc.InternalErrorException;
import org.stellar.anchor.api.rpc.ActionMethod;
import org.stellar.anchor.api.rpc.RpcRequest;
import org.stellar.anchor.api.rpc.RpcResponse;
import org.stellar.anchor.platform.action.ActionHandler;

public class ActionService {

  private final Map<ActionMethod, ActionHandler<?>> actionHandlerMap;

  public ActionService(List<ActionHandler<?>> actionHandlers) {
    this.actionHandlerMap =
        actionHandlers.stream().collect(toMap(ActionHandler::getActionType, identity()));
  }

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
        .collect(toList());
  }

  private Object processRpcCall(RpcRequest rpcCall) throws AnchorException {
    debugF("Started processing of RPC call with method [{}]", rpcCall.getMethod());
    ActionHandler<?> actionHandler = actionHandlerMap.get(ActionMethod.from(rpcCall.getMethod()));
    if (actionHandler == null) {
      throw new InternalErrorException(
          String.format("Action[%s] handler is not found", rpcCall.getMethod()));
    }
    // TODO: Add response
    actionHandler.handle(rpcCall.getParams());
    return null;
  }
}

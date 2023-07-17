package org.stellar.anchor.platform.service;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.stellar.anchor.platform.utils.RpcUtil.getRpcErrorResponse;
import static org.stellar.anchor.platform.utils.RpcUtil.getRpcSuccessResponse;
import static org.stellar.anchor.platform.utils.RpcUtil.validateRpcRequest;
import static org.stellar.anchor.util.Log.debugF;
import static org.stellar.anchor.util.Log.errorEx;

import java.util.List;
import java.util.Map;
import org.stellar.anchor.api.exception.AnchorException;
import org.stellar.anchor.api.exception.BadRequestException;
import org.stellar.anchor.api.exception.rpc.InternalErrorException;
import org.stellar.anchor.api.exception.rpc.MethodNotFoundException;
import org.stellar.anchor.api.exception.rpc.RpcException;
import org.stellar.anchor.api.rpc.RpcRequest;
import org.stellar.anchor.api.rpc.RpcResponse;
import org.stellar.anchor.api.rpc.action.ActionMethod;
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
            rc -> {
              final Object rpcId = rc.getId();
              try {
                validateRpcRequest(rc);
                return getRpcSuccessResponse(rpcId, processRpcCall(rc));
              } catch (RpcException ex) {
                errorEx(
                    String.format(
                        "An RPC error occurred while processing an RPC call with action[%s] and id[%s]",
                        rc.getMethod(), rpcId),
                    ex);
                return getRpcErrorResponse(rpcId, ex);
              } catch (BadRequestException ex) {
                return getRpcErrorResponse(rpcId, ex);
              } catch (Exception ex) {
                errorEx(
                    String.format(
                        "An internal error occurred while processing an RPC call with action[%s] and id[%s]",
                        rc.getMethod(), rpcId),
                    ex);
                return getRpcErrorResponse(rpcId, new InternalErrorException(ex.getMessage()));
              }
            })
        .collect(toList());
  }

  private Object processRpcCall(RpcRequest rpcCall) throws AnchorException {
    debugF("Started processing of RPC call with method [{}]", rpcCall.getMethod());
    ActionHandler<?> actionHandler = actionHandlerMap.get(ActionMethod.from(rpcCall.getMethod()));
    if (actionHandler == null) {
      throw new MethodNotFoundException(
          String.format("Action[%s] handler is not found", rpcCall.getMethod()));
    }
    return actionHandler.handle(rpcCall.getParams());
  }
}

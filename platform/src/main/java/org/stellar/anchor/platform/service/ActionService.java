package org.stellar.anchor.platform.service;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.stellar.anchor.platform.utils.RpcUtil.getRpcBatchLimitErrorResponse;
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
import org.stellar.anchor.platform.config.RpcConfig;

public class ActionService {

  private final Map<ActionMethod, ActionHandler<?>> actionHandlerMap;
  private final RpcConfig rpcConfig;

  public ActionService(List<ActionHandler<?>> actionHandlers, RpcConfig rpcConfig) {
    this.actionHandlerMap =
        actionHandlers.stream().collect(toMap(ActionHandler::getActionType, identity()));
    this.rpcConfig = rpcConfig;
  }

  public List<RpcResponse> handleRpcCalls(List<RpcRequest> rpcCalls) {
    if (rpcCalls.size() > rpcConfig.getActions().getBatchSizeLimit()) {
      return List.of(getRpcBatchLimitErrorResponse(rpcConfig.getActions().getBatchSizeLimit()));
    }

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
                return getRpcErrorResponse(rc, ex);
              } catch (BadRequestException ex) {
                return getRpcErrorResponse(rc, ex);
              } catch (Exception ex) {
                errorEx(
                    String.format(
                        "An internal error occurred while processing an RPC call with action[%s] and id[%s]",
                        rc.getMethod(), rpcId),
                    ex);
                return getRpcErrorResponse(rc, new InternalErrorException(ex.getMessage()));
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

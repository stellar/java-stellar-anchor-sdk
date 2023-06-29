package org.stellar.anchor.platform.action;

import static java.util.function.Function.identity;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.stellar.anchor.api.exception.AnchorException;
import org.stellar.anchor.platform.action.dto.ActionMethod;
import org.stellar.anchor.platform.action.dto.RpcRequest;
import org.stellar.anchor.platform.action.handlers.ActionHandler;

@Service
public class ActionResolver {

  private final Map<ActionMethod, ActionHandler> actionHandlerMap;

  public ActionResolver(List<ActionHandler> actionHandlers) {
    this.actionHandlerMap =
        actionHandlers.stream().collect(Collectors.toMap(ActionHandler::getActionType, identity()));
  }

  public void resolve(RpcRequest request) throws AnchorException {
    actionHandlerMap.get(request.getMethod()).handle(request.getParams());
  }
}

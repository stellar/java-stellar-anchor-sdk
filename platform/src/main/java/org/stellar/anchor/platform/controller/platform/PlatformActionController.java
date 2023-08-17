package org.stellar.anchor.platform.controller.platform;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.stellar.anchor.api.rpc.RpcRequest;
import org.stellar.anchor.api.rpc.RpcResponse;
import org.stellar.anchor.platform.service.ActionService;

@RestController
public class PlatformActionController {

  private final ActionService actionService;

  public PlatformActionController(ActionService actionService) {
    this.actionService = actionService;
  }

  @CrossOrigin(origins = "*")
  @RequestMapping(method = {RequestMethod.POST})
  @ResponseStatus(value = HttpStatus.OK)
  public List<RpcResponse> handleAction(@RequestBody List<RpcRequest> rpcCalls) {
    return actionService.handleRpcCalls(rpcCalls);
  }
}

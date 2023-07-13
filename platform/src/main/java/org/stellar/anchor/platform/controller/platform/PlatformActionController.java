package org.stellar.anchor.platform.controller.platform;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;
import static org.stellar.anchor.api.rpc.RpcErrorCode.findByErrorCode;

import java.util.List;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
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
  @RequestMapping(
      value = "/actions",
      method = {RequestMethod.POST})
  public ResponseEntity<List<RpcResponse>> handleAction(@RequestBody List<RpcRequest> rpcCalls) {
    List<RpcResponse> rpcResponses = actionService.handleRpcCalls(rpcCalls);

    HttpStatus httpStatus =
        rpcResponses.stream()
            .map(RpcResponse::getError)
            .filter(Objects::nonNull)
            .findFirst()
            .map(rpcError -> getHttpStatus(rpcError.getCode()))
            .orElse(OK);

    return new ResponseEntity<>(rpcResponses, httpStatus);
  }

  private HttpStatus getHttpStatus(int errorCode) {
    switch (findByErrorCode(errorCode)) {
      case METHOD_NOT_FOUND:
        return NOT_FOUND;
      case INVALID_REQUEST:
        return BAD_REQUEST;
      case INTERNAL_ERROR:
        return INTERNAL_SERVER_ERROR;
      default:
        return OK;
    }
  }
}

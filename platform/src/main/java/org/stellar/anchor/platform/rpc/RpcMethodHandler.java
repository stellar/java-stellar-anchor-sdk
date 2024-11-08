package org.stellar.anchor.platform.rpc;

import com.google.gson.Gson;
import org.stellar.anchor.api.exception.AnchorException;
import org.stellar.anchor.api.rpc.method.RpcMethod;
import org.stellar.anchor.api.rpc.method.RpcMethodParamsRequest;
import org.stellar.anchor.util.GsonUtils;

public abstract class RpcMethodHandler<T extends RpcMethodParamsRequest> {
  protected static final Gson gson = GsonUtils.getInstance();

  public abstract RpcMethod getRpcMethod();

  public abstract Object handle(Object requestParams) throws AnchorException;
}

package org.stellar.anchor.platform.event;

import static org.stellar.anchor.util.Log.debug;
import static org.stellar.anchor.util.Log.debugF;

import org.stellar.anchor.api.event.AnchorEvent;
import org.stellar.anchor.platform.config.ClientsConfig.ClientConfig;
import org.stellar.anchor.util.GsonUtils;

public class ClientStatusCallbackHandler extends EventHandler {
  private final ClientConfig clientConfig;

  public ClientStatusCallbackHandler(ClientConfig clientConfig) {
    super();
    this.clientConfig = clientConfig;
  }

  @Override
  void handleEvent(AnchorEvent event) {
    // TOOD: Implement calling the business server
    debugF("Sending event to client status api: {}", clientConfig.getCallbackUrl());
    debug(GsonUtils.getInstance().toJson(event));
  }
}

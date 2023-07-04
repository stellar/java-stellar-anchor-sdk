package org.stellar.anchor.platform.event;

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
    System.out.println("Sending event to callback api: " + clientConfig.getCallbackUrl());
    System.out.println(GsonUtils.getInstance().toJson(event));
  }
}

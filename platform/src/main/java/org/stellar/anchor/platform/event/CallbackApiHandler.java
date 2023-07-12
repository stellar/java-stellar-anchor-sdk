package org.stellar.anchor.platform.event;

import static org.stellar.anchor.util.Log.debug;
import static org.stellar.anchor.util.Log.debugF;

import org.stellar.anchor.api.event.AnchorEvent;
import org.stellar.anchor.platform.config.CallbackApiConfig;
import org.stellar.anchor.util.GsonUtils;

public class CallbackApiHandler extends EventHandler {

  private final CallbackApiConfig callbackApiConfig;

  CallbackApiHandler(CallbackApiConfig callbackApiConfig) {
    this.callbackApiConfig = callbackApiConfig;
  }

  @Override
  void handleEvent(AnchorEvent event) {
    // TOOD: Implement calling the business server
    debugF("Sending event to callback api: {}", callbackApiConfig.getBaseUrl());
    debug(GsonUtils.getInstance().toJson(event));
  }
}

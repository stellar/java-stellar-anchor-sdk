package org.stellar.anchor.platform.event;

import static org.stellar.anchor.util.Log.debugF;
import static org.stellar.anchor.util.OkHttpUtil.buildJsonRequestBody;
import static org.stellar.anchor.util.StringHelper.json;

import java.util.concurrent.TimeUnit;
import lombok.SneakyThrows;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.stellar.anchor.api.event.AnchorEvent;
import org.stellar.anchor.platform.config.CallbackApiConfig;

public class CallbackApiHandler extends EventHandler {
  private static final OkHttpClient httpClient =
      new OkHttpClient.Builder()
          .connectTimeout(10, TimeUnit.MINUTES)
          .readTimeout(10, TimeUnit.MINUTES)
          .writeTimeout(10, TimeUnit.MINUTES)
          .callTimeout(10, TimeUnit.MINUTES)
          .build();
  private final CallbackApiConfig callbackApiConfig;

  CallbackApiHandler(CallbackApiConfig callbackApiConfig) {
    this.callbackApiConfig = callbackApiConfig;
  }

  @SneakyThrows
  @Override
  void handleEvent(AnchorEvent event) {
    Request request = buildCallbackRequest(event);
    httpClient.newCall(request).execute();
    debugF("Sending event: {} to callback api: {}", json(event), callbackApiConfig.getBaseUrl());
  }

  Request buildCallbackRequest(AnchorEvent event) {
    return new Request.Builder()
        .url(callbackApiConfig.getBaseUrl() + "/callback")
        .post(buildJsonRequestBody(json(event)))
        .build();
  }
}

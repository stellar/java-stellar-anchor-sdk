package org.stellar.anchor.platform.event;

import static org.stellar.anchor.util.Log.debugF;
import static org.stellar.anchor.util.NetUtil.getDomainFromURL;
import static org.stellar.anchor.util.OkHttpUtil.buildJsonRequestBody;
import static org.stellar.anchor.util.StringHelper.json;

import java.util.Base64;
import java.util.concurrent.TimeUnit;
import lombok.SneakyThrows;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.stellar.anchor.api.event.AnchorEvent;
import org.stellar.anchor.platform.config.ClientsConfig.ClientConfig;
import org.stellar.anchor.platform.config.PropertySecretConfig;
import org.stellar.sdk.KeyPair;

public class ClientStatusCallbackHandler extends EventHandler {
  private static final OkHttpClient httpClient =
      new OkHttpClient.Builder()
          .connectTimeout(10, TimeUnit.MINUTES)
          .readTimeout(10, TimeUnit.MINUTES)
          .writeTimeout(10, TimeUnit.MINUTES)
          .callTimeout(10, TimeUnit.MINUTES)
          .build();
  private final ClientConfig clientConfig;

  public ClientStatusCallbackHandler(ClientConfig clientConfig) {
    super();
    this.clientConfig = clientConfig;
  }

  @SneakyThrows
  @Override
  void handleEvent(AnchorEvent event) {
    KeyPair signer = KeyPair.fromSecretSeed(new PropertySecretConfig().getSep10SigningSeed());
    Request request = buildSignedCallbackRequest(signer, event);
    httpClient.newCall(request).execute();
    debugF(
        "Sending event: {} to client status api: {}", json(event), clientConfig.getCallbackUrl());
  }

  @SneakyThrows
  Request buildSignedCallbackRequest(KeyPair signer, AnchorEvent event) {
    // Prepare the payload to sign
    String currentTs = String.valueOf(TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()));
    String domain = getDomainFromURL(clientConfig.getCallbackUrl());
    RequestBody requestBody = buildJsonRequestBody(json(event));
    String payload = currentTs + "." + domain + "." + requestBody;
    // Sign the payload using the Anchor private key
    // Base64 encode the signature
    String encodedSignature =
        new String(Base64.getEncoder().encode(signer.sign(payload.getBytes())));

    // Build the X-Stellar-Signature header
    return new Request.Builder()
        .url(clientConfig.getCallbackUrl())
        .header("Signature", String.format("t=%s, s=%s", currentTs, encodedSignature))
        .post(requestBody)
        .build();
  }
}

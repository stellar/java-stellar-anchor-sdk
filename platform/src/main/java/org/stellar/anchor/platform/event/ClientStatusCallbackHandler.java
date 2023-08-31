package org.stellar.anchor.platform.event;

import static org.stellar.anchor.sep24.Sep24Helper.fromTxn;
import static org.stellar.anchor.util.Log.debugF;
import static org.stellar.anchor.util.NetUtil.getDomainFromURL;
import static org.stellar.anchor.util.OkHttpUtil.buildJsonRequestBody;
import static org.stellar.anchor.util.StringHelper.json;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.Base64;
import java.util.concurrent.TimeUnit;
import lombok.SneakyThrows;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.stellar.anchor.api.event.AnchorEvent;
import org.stellar.anchor.api.exception.AnchorException;
import org.stellar.anchor.api.exception.SepException;
import org.stellar.anchor.api.sep.sep24.Sep24GetTransactionResponse;
import org.stellar.anchor.asset.AssetService;
import org.stellar.anchor.config.SecretConfig;
import org.stellar.anchor.platform.config.ClientsConfig.ClientConfig;
import org.stellar.anchor.sep24.MoreInfoUrlConstructor;
import org.stellar.anchor.sep24.Sep24Transaction;
import org.stellar.anchor.sep24.Sep24TransactionStore;
import org.stellar.anchor.sep31.Sep31Transaction;
import org.stellar.anchor.sep31.Sep31TransactionStore;
import org.stellar.sdk.KeyPair;

public class ClientStatusCallbackHandler extends EventHandler {
  private static final OkHttpClient httpClient =
      new OkHttpClient.Builder()
          .connectTimeout(10, TimeUnit.MINUTES)
          .readTimeout(10, TimeUnit.MINUTES)
          .writeTimeout(10, TimeUnit.MINUTES)
          .callTimeout(10, TimeUnit.MINUTES)
          .build();
  private final SecretConfig secretConfig;
  private final ClientConfig clientConfig;
  private final Sep24TransactionStore sep24TransactionStore;
  private final Sep31TransactionStore sep31TransactionStore;
  private final AssetService assetService;
  private final MoreInfoUrlConstructor moreInfoUrlConstructor;

  public ClientStatusCallbackHandler(
      SecretConfig secretConfig,
      ClientConfig clientConfig,
      Sep24TransactionStore sep24TransactionStore,
      Sep31TransactionStore sep31TransactionStore,
      AssetService assetService,
      MoreInfoUrlConstructor moreInfoUrlConstructor) {
    super();
    this.secretConfig = secretConfig;
    this.clientConfig = clientConfig;
    this.sep24TransactionStore = sep24TransactionStore;
    this.sep31TransactionStore = sep31TransactionStore;
    this.assetService = assetService;
    this.moreInfoUrlConstructor = moreInfoUrlConstructor;
  }

  @Override
  boolean handleEvent(AnchorEvent event) throws IOException {
    if (event.getTransaction() != null) {
      KeyPair signer = KeyPair.fromSecretSeed(secretConfig.getSep10SigningSeed());
      Request request = buildHttpRequest(signer, event);
      Response response = httpClient.newCall(request).execute();
      debugF(
          "Sending event: {} to client status api: {}", json(event), clientConfig.getCallbackUrl());
      if (response.code() < 200 || response.code() >= 400) {
        return false;
      }
    }
    return true;
  }

  @SneakyThrows
  Request buildHttpRequest(KeyPair signer, AnchorEvent event) {
    String payload = getPayload(event);
    return buildHttpRequest(signer, payload, clientConfig.getCallbackUrl());
  }

  @SneakyThrows
  public static Request buildHttpRequest(KeyPair signer, String payload, String url) {
    // Prepare the payload to sign
    String domain = getDomainFromURL(url);
    String currentTs = String.valueOf(TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()));
    String payloadToSign = currentTs + "." + domain + "." + payload;
    // Sign the payload using the Anchor private key
    // Base64 encode the signature
    String encodedSignature =
        new String(Base64.getEncoder().encode(signer.sign(payloadToSign.getBytes())));

    // Build the X-Stellar-Signature header
    return new Request.Builder()
        .url(url)
        .header("Signature", String.format("t=%s, s=%s", currentTs, encodedSignature))
        .post(buildJsonRequestBody(payload))
        .build();
  }

  private String getPayload(AnchorEvent event)
      throws AnchorException, MalformedURLException, URISyntaxException {
    switch (event.getTransaction().getSep()) {
      case SEP_24:
        Sep24Transaction sep24Txn =
            sep24TransactionStore.findByTransactionId(event.getTransaction().getId());
        Sep24GetTransactionResponse txn24Response =
            Sep24GetTransactionResponse.of(fromTxn(assetService, moreInfoUrlConstructor, sep24Txn));
        return json(txn24Response);
      case SEP_31:
        Sep31Transaction sep31Txn =
            sep31TransactionStore.findByTransactionId(event.getTransaction().getId());
        return json(sep31Txn.toSep31GetTransactionResponse());
      default:
        throw new SepException("Only SEP-24 and SEP-31 are supported");
    }
  }
}

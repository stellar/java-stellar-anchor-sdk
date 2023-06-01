package org.stellar.anchor.platform.service;

import static org.stellar.anchor.sep24.Sep24Service.INTERACTIVE_URL_JWT_REQUIRED_FIELDS_FROM_REQUEST;
import static org.stellar.anchor.sep9.Sep9Fields.extractSep9Fields;
import static org.stellar.anchor.util.AssetHelper.*;
import static org.stellar.anchor.util.StringHelper.isEmpty;

import java.net.URI;
import java.time.Instant;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import javax.crypto.spec.IvParameterSpec;
import lombok.SneakyThrows;
import org.apache.http.client.utils.URIBuilder;
import org.stellar.anchor.api.exception.InvalidConfigException;
import org.stellar.anchor.auth.JwtService;
import org.stellar.anchor.auth.Sep24InteractiveUrlJwt;
import org.stellar.anchor.platform.config.PropertySep24Config.InteractiveUrlConfig;
import org.stellar.anchor.sep24.InteractiveUrlConstructor;
import org.stellar.anchor.sep24.Sep24Transaction;
import org.stellar.anchor.util.AESUtil;

public class SimpleInteractiveUrlConstructor extends InteractiveUrlConstructor {
  private final InteractiveUrlConfig config;
  private final JwtService jwtService;
  @Nullable private AESUtil aesUtil;
  private final String secret;

  public SimpleInteractiveUrlConstructor(
      InteractiveUrlConfig config, JwtService jwtService, String secret) {
    this.config = config;
    this.jwtService = jwtService;
    this.secret = secret;
  }

  @Override
  @SneakyThrows
  public String construct(Sep24Transaction txn, Map<String, String> request) {
    String token = constructToken(txn, request);
    String baseUrl = config.getBaseUrl();
    URI uri = new URI(baseUrl);
    return new URIBuilder()
        .setScheme(uri.getScheme())
        .setHost(uri.getHost())
        .setPort(uri.getPort())
        .setPath(uri.getPath())
        .addParameter("transaction_id", txn.getTransactionId())
        // Add the JWT token
        .addParameter("token", token)
        .build()
        .toURL()
        .toString();
  }

  @SneakyThrows
  String constructToken(Sep24Transaction txn, Map<String, String> request) {
    String account =
        (isEmpty(txn.getSep10AccountMemo()))
            ? txn.getSep10Account()
            : txn.getSep10Account() + ":" + txn.getSep10AccountMemo();
    Sep24InteractiveUrlJwt token =
        new Sep24InteractiveUrlJwt(
            account,
            txn.getTransactionId(),
            Instant.now().getEpochSecond() + config.getJwtExpiration(),
            txn.getClientDomain());

    Map<String, String> data = new HashMap<>();
    // Add sep-9 fields from request
    Map<String, String> sep9 = extractSep9Fields(request);
    data.putAll(encodeSep9(sep9));
    // Add required JWT fields from request
    data.putAll(extractRequiredJwtFieldsFromRequest(request));
    // Add fields defined in txnFields
    UrlConstructorHelper.addTxnFields(data, txn, config.getTxnFields());

    token.claim("data", data);
    return jwtService.encode(token);
  }

  private Map<String, String> encodeSep9(Map<String, String> sep9) throws Exception {
    if (sep9.isEmpty()) {
      return Collections.emptyMap();
    }

    Map<String, String> encrypted = new HashMap<>();

    if (aesUtil == null) {
      if (secret == null) {
        throw new InvalidConfigException(
            "Please provide the secret before encoding JWT for Sep24 interactive url");
      }

      aesUtil = new AESUtil(secret);
    }

    IvParameterSpec iv = aesUtil.getIv();

    for (Map.Entry<String, String> entry : sep9.entrySet()) {
      String k = entry.getKey();
      String v = entry.getValue();
      encrypted.put(k, aesUtil.encrypt(v, iv));
    }

    encrypted.put("iv", Base64.getEncoder().encodeToString(iv.getIV()));

    return encrypted;
  }

  public static Map<String, String> extractRequiredJwtFieldsFromRequest(
      Map<String, String> request) {
    Map<String, String> fields = new HashMap<>();
    for (Map.Entry<String, String> entry : request.entrySet()) {
      if (INTERACTIVE_URL_JWT_REQUIRED_FIELDS_FROM_REQUEST.contains(entry.getKey())) {
        fields.put(entry.getKey(), entry.getValue());
      }
    }

    String asset = getAssetId(request.get("asset_code"), request.get("asset_issuer"));
    if (!isEmpty(asset)) {
      fields.put("asset", asset);
    }

    return fields;
  }
}

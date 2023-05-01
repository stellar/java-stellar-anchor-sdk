package org.stellar.anchor.platform.service;

import static org.stellar.anchor.sep24.Sep24Service.INTERACTIVE_URL_JWT_REQUIRED_FIELDS;
import static org.stellar.anchor.sep9.Sep9Fields.extractSep9Fields;
import static org.stellar.anchor.util.StringHelper.isEmpty;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import lombok.SneakyThrows;
import org.apache.http.client.utils.URIBuilder;
import org.stellar.anchor.auth.JwtService;
import org.stellar.anchor.auth.Sep24InteractiveUrlJwt;
import org.stellar.anchor.platform.config.PropertySep24Config.InteractiveUrlConfig;
import org.stellar.anchor.sep24.InteractiveUrlConstructor;
import org.stellar.anchor.sep24.Sep24Transaction;

public class SimpleInteractiveUrlConstructor extends InteractiveUrlConstructor {
  private final InteractiveUrlConfig config;
  private final JwtService jwtService;

  public SimpleInteractiveUrlConstructor(InteractiveUrlConfig config, JwtService jwtService) {
    this.config = config;
    this.jwtService = jwtService;
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
    data.putAll(extractSep9Fields(request));
    data.putAll(extractInteractiveUrlJWTFields(request));

    // Add fields defined in txnFields
    UrlConstructorHelper.addTxnFields(data, txn, config.getTxnFields());
    token.claim("data", data);
    return jwtService.encode(token);
  }

  public static Map<String, String> extractInteractiveUrlJWTFields(Map<String, String> request) {
    Map<String, String> fields = new HashMap<>();
    for (Map.Entry<String, String> entry : request.entrySet()) {
      if (INTERACTIVE_URL_JWT_REQUIRED_FIELDS.contains(entry.getKey())) {
        fields.put(entry.getKey(), entry.getValue());
      }
    }
    return fields;
  }
}

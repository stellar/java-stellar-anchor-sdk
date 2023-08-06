package org.stellar.anchor.platform.service;

import static org.stellar.anchor.platform.config.PropertySep24Config.MoreInfoUrlConfig;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import lombok.SneakyThrows;
import org.apache.http.client.utils.URIBuilder;
import org.stellar.anchor.auth.JwtService;
import org.stellar.anchor.auth.Sep24MoreInfoUrlJwt;
import org.stellar.anchor.sep24.MoreInfoUrlConstructor;
import org.stellar.anchor.sep24.Sep24Transaction;

public class SimpleMoreInfoUrlConstructor extends MoreInfoUrlConstructor {
  private final MoreInfoUrlConfig config;
  private final JwtService jwtService;

  public SimpleMoreInfoUrlConstructor(MoreInfoUrlConfig config, JwtService jwtService) {
    this.config = config;
    this.jwtService = jwtService;
  }

  @Override
  @SneakyThrows
  public String construct(Sep24Transaction txn) {
    String account = UrlConstructorHelper.getAccount(txn);
    Sep24MoreInfoUrlJwt token =
        new Sep24MoreInfoUrlJwt(
            account,
            txn.getTransactionId(),
            Instant.now().getEpochSecond() + config.getJwtExpiration(),
            txn.getClientDomain());

    Map<String, String> data = new HashMap<>();

    // Add fields defined in txnFields
    UrlConstructorHelper.addTxnFields(data, txn, config.getTxnFields());
    token.claim("data", data);

    String baseUrl = config.getBaseUrl();
    URI uri = new URI(baseUrl);
    return new URIBuilder()
        .setScheme(uri.getScheme())
        .setHost(uri.getHost())
        .setPort(uri.getPort())
        .setPath(uri.getPath())
        .addParameter("transaction_id", txn.getTransactionId())
        .addParameter("token", jwtService.encode(token))
        .build()
        .toURL()
        .toString();
  }
}
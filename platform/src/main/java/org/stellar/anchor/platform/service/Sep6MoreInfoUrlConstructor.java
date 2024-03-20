package org.stellar.anchor.platform.service;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import lombok.SneakyThrows;
import org.apache.http.client.utils.URIBuilder;
import org.stellar.anchor.MoreInfoUrlConstructor;
import org.stellar.anchor.SepTransaction;
import org.stellar.anchor.auth.JwtService;
import org.stellar.anchor.auth.Sep6MoreInfoUrlJwt;
import org.stellar.anchor.platform.config.MoreInfoUrlConfig;
import org.stellar.anchor.platform.config.PropertyClientsConfig;
import org.stellar.anchor.sep6.Sep6Transaction;
import org.stellar.anchor.util.ConfigHelper;

public class Sep6MoreInfoUrlConstructor extends MoreInfoUrlConstructor {
  PropertyClientsConfig clientsConfig;
  private final MoreInfoUrlConfig config;
  private final JwtService jwtService;

  public Sep6MoreInfoUrlConstructor(
      PropertyClientsConfig clientsConfig, MoreInfoUrlConfig config, JwtService jwtService) {
    this.clientsConfig = clientsConfig;
    this.config = config;
    this.jwtService = jwtService;
  }

  @Override
  public String construct(SepTransaction txn) {
    return construct((Sep6Transaction) txn);
  }

  @SneakyThrows
  public String construct(Sep6Transaction txn) {
    PropertyClientsConfig.ClientConfig clientConfig =
        ConfigHelper.getClientConfig(clientsConfig, txn.getClientDomain(), txn.getSep10Account());

    Sep6MoreInfoUrlJwt token =
        new Sep6MoreInfoUrlJwt(
            UrlConstructorHelper.getAccount(txn.getMemo(), txn.getSep10Account()),
            txn.getTransactionId(),
            Instant.now().getEpochSecond() + config.getJwtExpiration(),
            txn.getClientDomain(),
            clientConfig != null ? clientConfig.getName() : null);

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

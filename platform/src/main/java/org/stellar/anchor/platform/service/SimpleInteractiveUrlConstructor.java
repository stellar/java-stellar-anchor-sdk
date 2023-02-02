package org.stellar.anchor.platform.service;

import static org.stellar.anchor.util.StringHelper.camelToSnake;
import static org.stellar.anchor.util.StringHelper.snakeToCamelCase;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import lombok.SneakyThrows;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.http.client.utils.URIBuilder;
import org.stellar.anchor.auth.JwtService;
import org.stellar.anchor.auth.Sep24InteractiveUrlJwt;
import org.stellar.anchor.platform.config.PropertySep24Config.InteractiveUrlConfig;
import org.stellar.anchor.sep24.InteractiveUrlConstructor;
import org.stellar.anchor.sep24.Sep24Transaction;
import org.stellar.anchor.util.StringHelper;

public class SimpleInteractiveUrlConstructor extends InteractiveUrlConstructor {
  private final InteractiveUrlConfig config;
  private final JwtService jwtService;

  public SimpleInteractiveUrlConstructor(InteractiveUrlConfig config, JwtService jwtService) {
    this.config = config;
    this.jwtService = jwtService;
  }

  @Override
  @SneakyThrows
  public String construct(Sep24Transaction txn, String lang, HashMap<String, String> sep9Fields) {
    Sep24InteractiveUrlJwt token =
        new Sep24InteractiveUrlJwt(
            txn.getTransactionId(),
            Instant.now().getEpochSecond() + config.getJwtExpiration(),
            txn.getClientDomain());
    String baseUrl = config.getBaseUrl();
    URI uri = new URI(baseUrl);

    Map<String, String> data = new HashMap<>();
    // Add lang field
    if (lang != null) {
      data.put("lang", lang);
    }

    for (Map.Entry<String, String> field : sep9Fields.entrySet()) {
      data.put(field.getKey(), field.getValue());
    }

    // Add fields defined in txnFields
    for (String field : config.getTxnFields()) {
      try {
        field = camelToSnake(field);
        String value = BeanUtils.getProperty(txn, snakeToCamelCase(field));
        if (!StringHelper.isEmpty((value))) {
          data.put(field, value);
        }
      } catch (Exception e) {
        // give up
      }
    }

    token.claim("data", data);

    String tokenCipher = jwtService.encode(token);

    return new URIBuilder()
        .setScheme(uri.getScheme())
        .setHost(uri.getHost())
        .setPort(uri.getPort())
        .setPath(uri.getPath())
        .addParameter("transaction_id", txn.getTransactionId())
        .addParameter("token", tokenCipher)
        .build()
        .toURL()
        .toString();
  }
}

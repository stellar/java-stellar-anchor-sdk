package org.stellar.anchor.platform.service;

import static org.stellar.anchor.util.StringHelper.snakeToCamelCase;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.http.client.utils.URIBuilder;
import org.stellar.anchor.auth.JwtService;
import org.stellar.anchor.auth.Sep10Jwt;
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

  public String construct(
          Sep10Jwt token, Sep24Transaction txn, String lang, HashMap<String, String> sep9Fields)
      throws URISyntaxException, MalformedURLException {

    String baseUrl = config.getBaseUrl();

    URI uri = new URI(baseUrl);

    URIBuilder builder =
        new URIBuilder()
            .setScheme(uri.getScheme())
            .setHost(uri.getHost())
            .setPort(uri.getPort())
            .setPath(uri.getPath())
            .addParameter("transaction_id", txn.getTransactionId())
            .addParameter("token", jwtService.encode(token));

    // Add lang field
    if (lang != null) {
      builder.addParameter("lang", lang);
    }

    // Add Sep9 fields
    sep9Fields.forEach(builder::addParameter);

    // Add fields defined in txnFields
    for (String field : config.getTxnFields()) {
      try {
        field = snakeToCamelCase(field);
        String value = BeanUtils.getProperty(txn, snakeToCamelCase(field));
        if (!StringHelper.isEmpty((value))) {
          builder.addParameter(field, value);
        }
      } catch (Exception e) {
        // give up
      }
    }

    return builder.build().toURL().toString();
  }
}

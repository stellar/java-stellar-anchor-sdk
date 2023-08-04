package org.stellar.anchor.platform.service;

import static org.stellar.anchor.sep24.Sep24Service.INTERACTIVE_URL_JWT_REQUIRED_FIELDS_FROM_REQUEST;
import static org.stellar.anchor.sep9.Sep9Fields.extractSep9Fields;
import static org.stellar.anchor.util.AssetHelper.*;
import static org.stellar.anchor.util.Log.debugF;
import static org.stellar.anchor.util.StringHelper.isEmpty;

import com.google.gson.Gson;
import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import lombok.SneakyThrows;
import org.apache.http.client.utils.URIBuilder;
import org.stellar.anchor.api.callback.CustomerIntegration;
import org.stellar.anchor.api.callback.PutCustomerRequest;
import org.stellar.anchor.api.exception.AnchorException;
import org.stellar.anchor.auth.JwtService;
import org.stellar.anchor.auth.Sep24InteractiveUrlJwt;
import org.stellar.anchor.platform.config.ClientsConfig;
import org.stellar.anchor.platform.config.PropertySep24Config;
import org.stellar.anchor.sep24.InteractiveUrlConstructor;
import org.stellar.anchor.sep24.Sep24Transaction;
import org.stellar.anchor.util.GsonUtils;

public class SimpleInteractiveUrlConstructor extends InteractiveUrlConstructor {
  public static final String FORWARD_KYC_CUSTOMER_TYPE = "sep24-customer";

  private final ClientsConfig clientsConfig;
  private final PropertySep24Config sep24Config;
  private final CustomerIntegration customerIntegration;
  private final JwtService jwtService;

  public SimpleInteractiveUrlConstructor(
      ClientsConfig clientsConfig,
      PropertySep24Config sep24Config,
      CustomerIntegration customerIntegration,
      JwtService jwtService) {
    this.clientsConfig = clientsConfig;
    this.sep24Config = sep24Config;
    this.customerIntegration = customerIntegration;
    this.jwtService = jwtService;
  }

  @Override
  @SneakyThrows
  public String construct(Sep24Transaction txn, Map<String, String> request) {
    // If there are KYC fields in the request, they will be forwarded to PUT /customer before
    // returning the token.
    forwardKycFields(request);

    // construct the token
    String token = constructToken(txn, request);

    // construct the URL
    String baseUrl = sep24Config.getInteractiveUrl().getBaseUrl();
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
    ClientsConfig.ClientConfig clientConfig =
        UrlConstructorHelper.getClientConfig(clientsConfig, txn);

    debugF(
        "Resolving configs for token construct. Got config: {}, all configs: {}",
        clientConfig,
        clientsConfig);

    Sep24InteractiveUrlJwt token =
        new Sep24InteractiveUrlJwt(
            UrlConstructorHelper.getAccount(txn),
            txn.getTransactionId(),
            Instant.now().getEpochSecond() + sep24Config.getInteractiveUrl().getJwtExpiration(),
            txn.getClientDomain(),
            clientConfig != null ? clientConfig.getName() : null);

    // Add required JWT fields from request
    Map<String, String> data = new HashMap<>(extractRequiredJwtFieldsFromRequest(request));
    // Add fields defined in txnFields
    UrlConstructorHelper.addTxnFields(data, txn, sep24Config.getInteractiveUrl().getTxnFields());

    token.claim("data", data);
    return jwtService.encode(token);
  }

  void forwardKycFields(Map<String, String> request) throws AnchorException {
    if (sep24Config.getKycFieldsForwarding().isEnabled()) {
      // Get sep-9 fields from request
      Map<String, String> sep9 = extractSep9Fields(request);
      // Putting SEP-9 into JWT exposes PII
      if (!sep9.isEmpty()) {
        Gson gson = GsonUtils.getInstance();
        String gsonRequest = gson.toJson(sep9);
        PutCustomerRequest putCustomerRequest =
            gson.fromJson(gsonRequest, PutCustomerRequest.class);
        putCustomerRequest.setType(FORWARD_KYC_CUSTOMER_TYPE);
        // forward kyc fields to PUT /customer
        customerIntegration.putCustomer(putCustomerRequest);
      }
    }
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

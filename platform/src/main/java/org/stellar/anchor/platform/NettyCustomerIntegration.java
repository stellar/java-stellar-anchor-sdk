package org.stellar.anchor.platform;

import com.google.gson.Gson;
import io.netty.handler.codec.http.QueryStringEncoder;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import org.stellar.anchor.exception.AnchorException;
import org.stellar.anchor.integration.customer.CustomerIntegration;
import org.stellar.platform.apis.callbacks.responses.DeleteCustomerResponse;
import org.stellar.platform.apis.callbacks.responses.GetCustomerResponse;
import org.stellar.platform.apis.callbacks.responses.PutCustomerResponse;
import reactor.core.publisher.Mono;
import reactor.netty.ByteBufMono;
import reactor.netty.http.client.HttpClient;

public class NettyCustomerIntegration implements CustomerIntegration {
  private final String baseUri;
  private final Gson gson = new Gson();

  public NettyCustomerIntegration(String baseUri) {
    try {
      new URI(baseUri);
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException("invalid 'baseUri'");
    }
    this.baseUri = baseUri;
  }

  @Override
  public Mono<org.stellar.anchor.dto.sep12.GetCustomerResponse> getCustomer(
      org.stellar.anchor.dto.sep12.GetCustomerRequest request) {
    // TODO: handle unexpected responses
    QueryStringEncoder encoder = new QueryStringEncoder(baseUri + "/customer");
    if (request.getId() != null) {
      encoder.addParam("id", request.getId());
    } else {
      encoder.addParam("account", request.getAccount());
      if (request.getMemo() != null && request.getMemoType() != null) {
        encoder.addParam("memo", request.getMemo());
        encoder.addParam("memo_type", request.getMemoType());
      }
    }
    if (request.getType() != null) {
      encoder.addParam("type", request.getType());
    }
    HttpClient client = HttpClient.create();
    return client
        .get()
        .uri(encoder.toString())
        .responseSingle((response, bytes) -> bytes.asString())
        .map(body -> gson.fromJson(body, GetCustomerResponse.class))
        .map(
            getCustomerResponse -> {
              updateCustomerStatus(
                  getCustomerResponse.getId(), request.getType(), getCustomerResponse.getStatus());
              return ResponseConverter.getCustomer(getCustomerResponse);
            });
  }

  @Override
  public Mono<org.stellar.anchor.dto.sep12.PutCustomerResponse> putCustomer(
      org.stellar.anchor.dto.sep12.PutCustomerRequest request) {
    // TODO: handle unexpected responses
    HttpClient client = HttpClient.create();
    return client
        .post()
        .uri(baseUri + "/customer")
        .send(ByteBufMono.fromString(Mono.just(gson.toJson(request))))
        .responseSingle((response, bytes) -> bytes.asString())
        .map(body -> gson.fromJson(body, PutCustomerResponse.class))
        .map(
            putCustomerResponse -> {
              updateCustomerStatus(
                  putCustomerResponse.getId(), request.getType(), putCustomerResponse.getStatus());
              return ResponseConverter.putCustomer(putCustomerResponse);
            });
  }

  @Override
  public Mono<Void> deleteCustomer(org.stellar.anchor.dto.sep12.DeleteCustomerRequest request) {
    // TODO: handle unexpected responses
    HttpClient client = HttpClient.create();
    return client
        .delete()
        .uri(baseUri + "/customer")
        .send(ByteBufMono.fromString(Mono.just(gson.toJson(request))))
        .responseSingle((response, bytes) -> bytes.asString())
        .map(body -> gson.fromJson(body, DeleteCustomerResponse.class))
        .flatMap(
            deleteCustomerResponse -> {
              updateCustomerStatus(deleteCustomerResponse.getId(), null, null);
              return Mono.empty();
            });
  }

  @Override
  public Mono<org.stellar.anchor.integration.customer.PutCustomerVerificationResponse>
      putVerification(
          org.stellar.anchor.integration.customer.PutCustomerVerificationRequest request) {
    // the Platform Callback API doesn't support verification.
    // if it does in the future we can implement this method
    throw new AnchorException(501, "not implemented");
  }

  private void updateCustomerStatus(String id, String type, String status) {
    // TODO: implement
  }
}

class ResponseConverter {
  public static org.stellar.anchor.dto.sep12.GetCustomerResponse getCustomer(
      GetCustomerResponse response) {
    org.stellar.anchor.dto.sep12.GetCustomerResponse integrationResponse =
        new org.stellar.anchor.dto.sep12.GetCustomerResponse();
    integrationResponse.setId(response.getId());
    integrationResponse.setStatus(
        org.stellar.anchor.dto.sep12.Sep12Status.valueOf(response.getStatus()));
    integrationResponse.setFields(convertFields(response.getFields()));
    integrationResponse.setProvidedFields(convertProvidedFields(response.getProvidedFields()));
    integrationResponse.setMessage(response.getMessage());
    return integrationResponse;
  }

  public static org.stellar.anchor.dto.sep12.PutCustomerResponse putCustomer(
      org.stellar.platform.apis.callbacks.responses.PutCustomerResponse response) {
    org.stellar.anchor.dto.sep12.PutCustomerResponse integrationResponse =
        new org.stellar.anchor.dto.sep12.PutCustomerResponse();
    integrationResponse.setId(response.getId());
    return integrationResponse;
  }

  private static Map<String, org.stellar.anchor.dto.sep12.ProvidedField> convertProvidedFields(
      Map<String, org.stellar.platform.apis.shared.ProvidedField> fields) {
    Map<String, org.stellar.anchor.dto.sep12.ProvidedField> integrationFields = new HashMap<>();
    for (Map.Entry<String, org.stellar.platform.apis.shared.ProvidedField> entry :
        fields.entrySet()) {
      org.stellar.anchor.dto.sep12.ProvidedField field =
          new org.stellar.anchor.dto.sep12.ProvidedField();
      field.setType(org.stellar.anchor.dto.sep12.Field.Type.valueOf(entry.getValue().getType()));
      field.setDescription(entry.getValue().getDescription());
      field.setChoices(entry.getValue().getChoices());
      field.setOptional(entry.getValue().getOptional());
      field.setStatus(
          org.stellar.anchor.dto.sep12.Sep12Status.valueOf(entry.getValue().getStatus()));
      field.setError(entry.getValue().getError());
    }
    return integrationFields;
  }

  private static Map<String, org.stellar.anchor.dto.sep12.Field> convertFields(
      Map<String, org.stellar.platform.apis.shared.Field> fields) {
    Map<String, org.stellar.anchor.dto.sep12.Field> integrationFields = new HashMap<>();
    for (Map.Entry<String, org.stellar.platform.apis.shared.Field> entry : fields.entrySet()) {
      org.stellar.anchor.dto.sep12.Field field = new org.stellar.anchor.dto.sep12.Field();
      field.setType(org.stellar.anchor.dto.sep12.Field.Type.valueOf(entry.getValue().getType()));
      field.setDescription(entry.getValue().getDescription());
      field.setChoices(entry.getValue().getChoices());
      field.setOptional(entry.getValue().getOptional());
      integrationFields.put(entry.getKey(), field);
    }
    return integrationFields;
  }
}

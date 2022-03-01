package org.stellar.anchor.integration.customer;

import com.google.gson.Gson;
import io.netty.handler.codec.http.QueryStringEncoder;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import org.stellar.anchor.dto.sep12.*;
import org.stellar.anchor.dto.sep12.DeleteCustomerRequest;
import org.stellar.anchor.dto.sep12.PutCustomerRequest;
import org.stellar.anchor.dto.sep12.PutCustomerResponse;
import org.stellar.anchor.exception.AnchorException;
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
  public Mono<GetCustomerResponse> getCustomer(GetCustomerRequest request) {
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
        .map(
            body ->
                gson.fromJson(
                    body, org.stellar.platform.apis.callbacks.responses.GetCustomerResponse.class))
        .map(
            getCustomerResponse -> {
              updateCustomerStatus(
                  getCustomerResponse.getId(), request.getType(), getCustomerResponse.getStatus());
              return ResponseConverter.getCustomer(getCustomerResponse);
            });
  }

  @Override
  public Mono<PutCustomerResponse> putCustomer(PutCustomerRequest request) {
    // TODO: handle unexpected responses
    HttpClient client = HttpClient.create();
    return client
        .post()
        .uri(baseUri + "/customer")
        .send(ByteBufMono.fromString(Mono.just(gson.toJson(request))))
        .responseSingle((response, bytes) -> bytes.asString())
        .map(
            body ->
                gson.fromJson(
                    body, org.stellar.platform.apis.callbacks.responses.PutCustomerResponse.class))
        .map(
            putCustomerResponse -> {
              updateCustomerStatus(
                  putCustomerResponse.getId(), request.getType(), putCustomerResponse.getStatus());
              return ResponseConverter.putCustomer(putCustomerResponse);
            });
  }

  @Override
  public Mono<Void> deleteCustomer(DeleteCustomerRequest request) {
    // TODO: handle unexpected responses
    HttpClient client = HttpClient.create();
    return client
        .delete()
        .uri(baseUri + "/customer")
        .send(ByteBufMono.fromString(Mono.just(gson.toJson(request))))
        .responseSingle((response, bytes) -> bytes.asString())
        .map(
            body ->
                gson.fromJson(
                    body,
                    org.stellar.platform.apis.callbacks.responses.DeleteCustomerResponse.class))
        .flatMap(
            deleteCustomerResponse -> {
              updateCustomerStatus(deleteCustomerResponse.getId(), null, null);
              return Mono.empty();
            });
  }

  @Override
  public Mono<PutCustomerVerificationResponse> putVerification(
      PutCustomerVerificationRequest request) {
    // the Platform Callback API doesn't support verification.
    // if it does in the future we can implement this method
    throw new AnchorException(501, "not implemented");
  }

  private void updateCustomerStatus(String id, String type, String status) {
    // TODO: implement
  }
}

class ResponseConverter {
  public static GetCustomerResponse getCustomer(
      org.stellar.platform.apis.callbacks.responses.GetCustomerResponse response) {
    GetCustomerResponse integrationResponse = new GetCustomerResponse();
    integrationResponse.setId(response.getId());
    integrationResponse.setStatus(Sep12Status.valueOf(response.getStatus()));
    integrationResponse.setFields(convertFields(response.getFields()));
    integrationResponse.setProvidedFields(convertProvidedFields(response.getProvidedFields()));
    integrationResponse.setMessage(response.getMessage());
    return integrationResponse;
  }

  public static PutCustomerResponse putCustomer(
      org.stellar.platform.apis.callbacks.responses.PutCustomerResponse response) {
    PutCustomerResponse integrationResponse = new PutCustomerResponse();
    integrationResponse.setId(response.getId());
    return integrationResponse;
  }

  private static Map<String, ProvidedField> convertProvidedFields(
      Map<String, org.stellar.platform.apis.shared.ProvidedField> fields) {
    Map<String, ProvidedField> integrationFields = new HashMap<>();
    for (Map.Entry<String, org.stellar.platform.apis.shared.ProvidedField> entry :
        fields.entrySet()) {
      ProvidedField field = new ProvidedField();
      field.setType(Field.Type.valueOf(entry.getValue().getType()));
      field.setDescription(entry.getValue().getDescription());
      field.setChoices(entry.getValue().getChoices());
      field.setOptional(entry.getValue().getOptional());
      field.setStatus(Sep12Status.valueOf(entry.getValue().getStatus()));
      field.setError(entry.getValue().getError());
    }
    return integrationFields;
  }

  private static Map<String, Field> convertFields(
      Map<String, org.stellar.platform.apis.shared.Field> fields) {
    Map<String, Field> integrationFields = new HashMap<>();
    for (Map.Entry<String, org.stellar.platform.apis.shared.Field> entry : fields.entrySet()) {
      Field field = new Field();
      field.setType(Field.Type.valueOf(entry.getValue().getType()));
      field.setDescription(entry.getValue().getDescription());
      field.setChoices(entry.getValue().getChoices());
      field.setOptional(entry.getValue().getOptional());
      integrationFields.put(entry.getKey(), field);
    }
    return integrationFields;
  }
}

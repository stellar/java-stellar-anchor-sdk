package org.stellar.anchor.platform;

import com.google.gson.Gson;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.SneakyThrows;
import okhttp3.*;
import org.stellar.anchor.exception.*;
import org.stellar.anchor.integration.customer.CustomerIntegration;
import org.stellar.anchor.platform.model.Customer;
import org.stellar.anchor.platform.model.CustomerStatus;
import org.stellar.anchor.platform.repository.CustomerRepository;
import org.stellar.platform.apis.callbacks.requests.PutCustomerRequest;
import org.stellar.platform.apis.callbacks.responses.GetCustomerResponse;
import org.stellar.platform.apis.callbacks.responses.PutCustomerResponse;
import org.stellar.platform.apis.shared.ErrorResponse;

public class NettyCustomerIntegration implements CustomerIntegration {
  private final HttpUrl customerUrl;
  private final Gson gson = new Gson();
  private final CustomerRepository customerRepository;
  private final OkHttpClient httpClient;

  public NettyCustomerIntegration(String baseUri, CustomerRepository customerRepository) {
    try {
      new URI(baseUri);
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException("invalid 'baseUri'");
    }
    HttpUrl baseUrl = HttpUrl.parse(baseUri);
    if (baseUrl == null) {
      throw new IllegalArgumentException("invalid 'baseUri'");
    }
    this.customerUrl = baseUrl.newBuilder().addPathSegment("customer").build();
    this.customerRepository = customerRepository;
    // TODO
    // share http client across the platform
    // and set reasonable timeout & retry policies
    this.httpClient = new OkHttpClient();
  }

  @Override
  public org.stellar.anchor.dto.sep12.GetCustomerResponse getCustomer(
      org.stellar.anchor.dto.sep12.GetCustomerRequest request) throws AnchorException {
    HttpUrl.Builder urlBuilder = customerUrl.newBuilder();
    if (request.getId() != null) {
      urlBuilder.addQueryParameter("id", request.getId());
    } else {
      urlBuilder.addQueryParameter("account", request.getAccount());
      if (request.getMemo() != null && request.getMemoType() != null) {
        urlBuilder.addQueryParameter("memo", request.getMemo());
        urlBuilder.addQueryParameter("memo_type", request.getMemoType());
      }
    }
    if (request.getType() != null) {
      urlBuilder.addQueryParameter("type", request.getType());
    }

    Request callbackRequest = new Request.Builder().url(urlBuilder.build()).build();
    Response callbackResponse = getResponse(callbackRequest);
    String callbackResponseContent = getResponseContent(callbackResponse);

    if (callbackResponse.code() == 200) {
      GetCustomerResponse getCustomerResponse;
      try {
        getCustomerResponse = gson.fromJson(callbackResponseContent, GetCustomerResponse.class);
      } catch (Exception e) { // cannot read body from response
        throw new ServerErrorException("internal server error", e);
      }
      if (getCustomerResponse.getStatus() == null) {
        throw new ServerErrorException("internal server error");
      }
      updateCustomerStatus(
          getCustomerResponse.getId(), request.getType(), getCustomerResponse.getStatus());
      return ResponseConverter.getCustomer(getCustomerResponse);
    } else {
      throw handleErrorResponse(callbackResponseContent, callbackResponse.code());
    }
  }

  @Override
  public org.stellar.anchor.dto.sep12.PutCustomerResponse putCustomer(
      org.stellar.anchor.dto.sep12.PutCustomerRequest request) throws AnchorException {
    RequestBody requestBody =
        RequestBody.create(
            gson.toJson(request, PutCustomerRequest.class), MediaType.get("application/json"));
    Request callbackRequest = new Request.Builder().url(customerUrl).put(requestBody).build();
    Response callbackResponse = getResponse(callbackRequest);
    String callbackResponseContent = getResponseContent(callbackResponse);

    if (callbackResponse.code() == 200) {
      PutCustomerResponse putCustomerResponse;
      try {
        putCustomerResponse = gson.fromJson(callbackResponseContent, PutCustomerResponse.class);
      } catch (Exception e) {
        throw new ServerErrorException("internal server error", e);
      }
      if (putCustomerResponse.getStatus() == null) {
        throw new ServerErrorException("internal server error");
      }
      updateCustomerStatus(
          putCustomerResponse.getId(), request.getType(), putCustomerResponse.getStatus());
      return ResponseConverter.putCustomer(putCustomerResponse);
    } else {
      throw handleErrorResponse(callbackResponseContent, callbackResponse.code());
    }
  }

  @SneakyThrows
  @Override
  public void deleteCustomer(org.stellar.anchor.dto.sep12.DeleteCustomerRequest request) {
    // TODO
    throw new UnsupportedOperationException("not implemented");
  }

  @SneakyThrows
  @Override
  public org.stellar.anchor.integration.customer.PutCustomerVerificationResponse putVerification(
      org.stellar.anchor.integration.customer.PutCustomerVerificationRequest request) {
    // the Platform Callback API doesn't support verification.
    // if it does in the future we can implement this method
    throw new UnsupportedOperationException("not implemented");
  }

  Response getResponse(Request request) throws ServiceUnavailableException {
    try {
      return httpClient.newCall(request).execute();
    } catch (Exception e) { // IOException (connection error)
      throw new ServiceUnavailableException("service not available", e);
    }
  }

  String getResponseContent(Response response) throws ServerErrorException {
    String callbackResponseContent;
    try {
      ResponseBody callbackResponseBody = response.body();
      if (callbackResponseBody == null) {
        throw new RuntimeException("unable to fetch response body");
      }
      callbackResponseContent = callbackResponseBody.string();
    } catch (Exception e) {
      throw new ServerErrorException("internal server error", e);
    }
    return callbackResponseContent;
  }

  AnchorException handleErrorResponse(String responseContent, int responseCode) {
    ErrorResponse errorResponse;
    try {
      errorResponse = gson.fromJson(responseContent, ErrorResponse.class);
    } catch (Exception e) { // cannot read body from response
      return new ServerErrorException("internal server error", e);
    }
    AnchorException exception;
    if (responseCode == 400) {
      exception = new BadRequestException(errorResponse.getError());
    } else if (responseCode == 404) {
      exception = new NotFoundException(errorResponse.getError());
    } else {
      exception = new ServerErrorException("internal server error");
    }
    return exception;
  }

  void updateCustomerStatus(String id, String type, String status) {
    if (id == null) return;
    Optional<Customer> maybeCustomer = customerRepository.findById(id);
    if (maybeCustomer.isEmpty()) {
      Customer customer = new Customer();
      customer.setId(id);
      CustomerStatus customerStatus = new CustomerStatus();
      customerStatus.setStatus(status);
      customerStatus.setType(type);
      customerStatus.setCustomer(customer);
      customer.setStatuses(List.of(customerStatus));
      customerRepository.save(customer);
      return;
    }
    Customer customer = maybeCustomer.get();
    boolean statusFound = false;
    for (CustomerStatus s : customer.getStatuses()) {
      if (s.getType().equals(type)) {
        s.setStatus(status);
        statusFound = true;
        break;
      }
    }
    if (!statusFound) {
      CustomerStatus customerStatus = new CustomerStatus();
      customerStatus.setStatus(status);
      customerStatus.setType(type);
      customerStatus.setCustomer(customer);
      customer.getStatuses().add(customerStatus);
    }
    customerRepository.save(customer);
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

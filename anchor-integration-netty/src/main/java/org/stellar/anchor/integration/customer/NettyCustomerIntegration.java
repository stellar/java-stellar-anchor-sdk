package org.stellar.anchor.integration.customer;

import com.google.gson.Gson;
import io.netty.handler.codec.http.QueryStringEncoder;
import java.net.URI;
import java.net.URISyntaxException;
import org.stellar.anchor.dto.sep12.DeleteCustomerRequest;
import org.stellar.anchor.dto.sep12.GetCustomerRequest;
import org.stellar.anchor.dto.sep12.GetCustomerResponse;
import org.stellar.anchor.dto.sep12.PutCustomerRequest;
import org.stellar.anchor.dto.sep12.PutCustomerResponse;
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
    HttpClient client = HttpClient.create();
    return client
        .get()
        .uri(encoder.toString())
        .responseSingle((response, bytes) -> bytes.asString())
        .map(body -> gson.fromJson(body, GetCustomerResponse.class));
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
        .map(body -> gson.fromJson(body, PutCustomerResponse.class));
  }

  @Override
  public Mono<Void> deleteCustomer(DeleteCustomerRequest request) {
    // TODO: handle unexpeted responses
    HttpClient client = HttpClient.create();
    return client
        .delete()
        .uri(baseUri + "/customer")
        .send(ByteBufMono.fromString(Mono.just(gson.toJson(request))))
        .response()
        .flatMap(response -> Mono.empty());
  }

  @Override
  public Mono<PutCustomerVerificationResponse> putVerification(
      PutCustomerVerificationRequest request) {
    // the Platform Callback API doesn't support verification.
    // if it does in the future we can implement this method
    throw new RuntimeException("not implemented");
  }
}

package org.stellar.anchor.integration.customer;

import com.google.gson.Gson;
import io.netty.handler.codec.http.QueryStringEncoder;
import lombok.SneakyThrows;
import org.stellar.anchor.dto.sep12.DeleteCustomerRequest;
import org.stellar.anchor.dto.sep12.GetCustomerRequest;
import org.stellar.anchor.dto.sep12.GetCustomerResponse;
import org.stellar.anchor.dto.sep12.PutCustomerRequest;
import org.stellar.anchor.dto.sep12.PutCustomerResponse;
import org.stellar.anchor.dto.sep12.PutCustomerVerificationRequest;
import org.stellar.anchor.dto.sep12.PutCustomerVerificationResponse;
import reactor.core.publisher.Mono;
import reactor.netty.ByteBufMono;
import reactor.netty.http.client.HttpClient;

public class NettyCustomerIntegration implements CustomerIntegration {
  private final String endpoint;
  private final Gson gson = new Gson();

  public NettyCustomerIntegration(String endpoint) {
    this.endpoint = endpoint;
  }

  @SneakyThrows // TODO: This is temporary.
  @Override
  public Mono<GetCustomerResponse> getCustomer(GetCustomerRequest request) {
    QueryStringEncoder encoder = new QueryStringEncoder(endpoint + "/customers");
    encoder.addParam("account", request.getAccount());

    HttpClient client = HttpClient.create();
    return client
        .get()
        .uri(encoder.toUri())
        .responseSingle((response, bytes) -> bytes.asString())
        .map(body -> gson.fromJson(body, GetCustomerResponse.class));
  }

  @Override
  public Mono<PutCustomerResponse> putCustomer(PutCustomerRequest request) {
    // TODO: Refactor
    HttpClient client = HttpClient.create();
    return client
        .post()
        .uri(endpoint + "/customers")
        .send(ByteBufMono.fromString(Mono.just(gson.toJson(request))))
        .responseSingle((response, bytes) -> bytes.asString())
        .map(body -> gson.fromJson(body, PutCustomerResponse.class));
  }

  @Override
  public Mono<Void> delete(DeleteCustomerRequest request) {
    // TODO: Refactor
    HttpClient client = HttpClient.create();
    client
        .delete()
        .uri(endpoint + "/customers")
        .send(ByteBufMono.fromString(Mono.just(gson.toJson(request))))
        .response();
    return Mono.empty();
  }

  @Override
  public Mono<PutCustomerVerificationResponse> putVerification(
      PutCustomerVerificationRequest request) {
    return null;
  }
}

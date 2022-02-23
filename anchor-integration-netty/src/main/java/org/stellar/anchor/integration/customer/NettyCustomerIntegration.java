package org.stellar.anchor.integration.customer;

import org.stellar.anchor.dto.sep12.DeleteCustomerRequest;
import org.stellar.anchor.dto.sep12.GetCustomerRequest;
import org.stellar.anchor.dto.sep12.GetCustomerResponse;
import org.stellar.anchor.dto.sep12.PutCustomerRequest;
import org.stellar.anchor.dto.sep12.PutCustomerResponse;
import org.stellar.anchor.dto.sep12.PutCustomerVerificationRequest;
import org.stellar.anchor.dto.sep12.PutCustomerVerificationResponse;
import reactor.core.publisher.Mono;

public class NettyCustomerIntegration implements CustomerIntegration {
  private String endpoint;

  public NettyCustomerIntegration(String endpoint) {
    this.endpoint = endpoint;
  }

  @Override
  public Mono<GetCustomerResponse> getCustomer(GetCustomerRequest request) {
    return null;
  }

  @Override
  public Mono<PutCustomerResponse> putCustomer(PutCustomerRequest request) {
    return null;
  }

  @Override
  public Mono<Void> delete(DeleteCustomerRequest request) {
    return null;
  }

  @Override
  public Mono<PutCustomerVerificationResponse> putVerification(
      PutCustomerVerificationRequest request) {
    return null;
  }
}

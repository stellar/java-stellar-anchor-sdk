package org.stellar.anchor.sep12;

import org.stellar.anchor.dto.sep12.*;
import org.stellar.anchor.integration.customer.CustomerIntegration;
import reactor.core.publisher.Mono;

public class InProcessCustomerIntegration implements CustomerIntegration {
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

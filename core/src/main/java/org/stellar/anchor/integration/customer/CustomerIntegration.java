package org.stellar.anchor.integration.customer;

import org.stellar.anchor.dto.sep12.DeleteCustomerRequest;
import org.stellar.anchor.dto.sep12.GetCustomerRequest;
import org.stellar.anchor.dto.sep12.GetCustomerResponse;
import org.stellar.anchor.dto.sep12.PutCustomerRequest;
import org.stellar.anchor.dto.sep12.PutCustomerResponse;
import org.stellar.anchor.dto.sep12.PutCustomerVerificationRequest;
import org.stellar.anchor.dto.sep12.PutCustomerVerificationResponse;
import reactor.core.publisher.Mono;

public interface CustomerIntegration {
    Mono<GetCustomerResponse> get(GetCustomerRequest request);
    Mono<PutCustomerResponse> put(PutCustomerRequest request);
    Mono<Void> delete(DeleteCustomerRequest request);
    Mono<PutCustomerVerificationResponse> putVerification(PutCustomerVerificationRequest request);
}

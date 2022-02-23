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
  /**
   * Gets a customer.
   *
   * @param request The request to get a customer.
   * @return The GET customer response.
   */
  Mono<GetCustomerResponse> getCustomer(GetCustomerRequest request);

  /**
   * Puts a customer
   *
   * @param request The request to upload a customer.
   * @return The PUT customer response.
   */
  Mono<PutCustomerResponse> putCustomer(PutCustomerRequest request);

  /**
   * Deletes a customer.
   *
   * @param request The request to delete a customer.
   * @return Nothing
   */
  Mono<Void> delete(DeleteCustomerRequest request);

  /**
   * The request for verification.
   *
   * @param request The PUT request of a customer.
   * @return The response.
   */
  Mono<PutCustomerVerificationResponse> putVerification(PutCustomerVerificationRequest request);
}

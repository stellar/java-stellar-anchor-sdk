package org.stellar.anchor.api.callback;

import org.stellar.anchor.api.exception.AnchorException;
import org.stellar.anchor.api.sep.sep12.Sep12GetCustomerRequest;
import org.stellar.anchor.api.sep.sep12.Sep12GetCustomerResponse;
import org.stellar.anchor.api.sep.sep12.Sep12PutCustomerRequest;
import org.stellar.anchor.api.sep.sep12.Sep12PutCustomerResponse;

public interface CustomerIntegration {
  /**
   * Gets a customer.
   *
   * @param request The request to get a customer.
   * @return The GET customer response.
   * @throws AnchorException if error happens
   */
  Sep12GetCustomerResponse getCustomer(Sep12GetCustomerRequest request) throws AnchorException;

  /**
   * Puts a customer
   *
   * @param request The request to upload a customer.
   * @return The PUT customer response.
   * @throws AnchorException if error happens
   */
  Sep12PutCustomerResponse putCustomer(Sep12PutCustomerRequest request) throws AnchorException;

  /**
   * Deletes a customer.
   *
   * @param id The id of the customer to be deleted.
   * @throws AnchorException if error happens
   */
  void deleteCustomer(String id) throws AnchorException;

  /**
   * The request for verification.
   *
   * @param request The PUT request of a customer.
   * @return The response.
   * @throws AnchorException if error happens
   */
  PutCustomerVerificationResponse putVerification(PutCustomerVerificationRequest request)
      throws AnchorException;
}

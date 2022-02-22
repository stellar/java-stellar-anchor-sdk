package org.stellar.anchor.reference.service;

import org.springframework.stereotype.Service;
import org.stellar.anchor.dto.sep12.Sep12Status;
import org.stellar.anchor.exception.SepNotFoundException;
import org.stellar.anchor.integration.customer.*;
import org.stellar.anchor.reference.model.Customer;
import org.stellar.anchor.reference.repo.CustomerRepo;

import java.util.Optional;

@Service
public class CustomerService {
  private final CustomerRepo customerRepo;

  CustomerService(CustomerRepo customerRepo) {
    this.customerRepo = customerRepo;
  }

  public GetCustomerResponse getCustomer(GetCustomerRequest request) throws SepNotFoundException {
    Optional<Customer> customer = customerRepo.findById(request.getId());
    // TODO: customerRepo.findByAccount() can be used to check if the customer exists.
    if (customer.isEmpty()) {
      throw new SepNotFoundException(String.format("User [id=%s] was not found", request.getId()));
    }

    // Convert to response
    GetCustomerResponse response = new GetCustomerResponse();
    response.setId(customer.get().getId());
    // TODO: the status should be properly assigned.
    response.setStatus(Sep12Status.ACCEPTED);

    return response;
  }

  /**
   * If the customer is found, update the customer. Otherwise, create and insert a new customer.
   *
   * @param request the PUT customer request.
   * @return the PUT Customer response.
   */
  public PutCustomerResponse upsertCustomer(PutCustomerRequest request) {
    Optional<Customer> optCustomer = customerRepo.findById(request.getId());
    Customer customer;
    if (optCustomer.isEmpty()) {
      customer = new Customer();
    } else {
      customer = optCustomer.get();
    }

    // Sets the stellar account
    customer.setStellarAccount(request.getAccount());

    // Sets the first name
    String firstName = request.getSep9Fields().get("first_name");
    if (firstName != null) {
      customer.setFirstName(firstName);
    }

    // Sets the last name
    String lastName = request.getSep9Fields().get("last_name");
    if (lastName != null) {
      customer.setLastName(lastName);
    }

    // Convert to response
    PutCustomerResponse response = new PutCustomerResponse();
    response.setId(customer.getId());
    return response;
  }

  public void delete(DeleteCustomerRequest request) throws SepNotFoundException {
    Optional<Customer> customer = customerRepo.findByStellarAccount(request.getAccount());
    if (customer.isEmpty()) {
      throw new SepNotFoundException(
          String.format("Customer [account=%s] was not found", request.getAccount()));
    }
    customerRepo.delete(customer.get());

    throw new RuntimeException("Not implemented");
  }

  PutCustomerVerificationResponse putVerification(PutCustomerVerificationRequest request) {
    throw new RuntimeException("Not implemented");
  }
}

package org.stellar.anchor.reference.service;

import java.util.Optional;
import javax.ws.rs.NotFoundException;

import org.springframework.stereotype.Service;

import org.stellar.platform.apis.callbacks.requests.DeleteCustomerRequest;
import org.stellar.platform.apis.callbacks.requests.GetCustomerRequest;
import org.stellar.platform.apis.callbacks.requests.PutCustomerRequest;
import org.stellar.platform.apis.callbacks.responses.GetCustomerResponse;

import org.stellar.anchor.reference.model.Customer;
import org.stellar.anchor.reference.repo.CustomerRepo;
import org.stellar.platform.apis.callbacks.responses.PutCustomerResponse;

@Service
public class CustomerService {
  private final CustomerRepo customerRepo;

  CustomerService(CustomerRepo customerRepo) {
    this.customerRepo = customerRepo;
  }

  public GetCustomerResponse getCustomer(GetCustomerRequest request) throws NotFoundException {
    Optional<Customer> maybeCustomer;
    if (request.getId() != null) {
      maybeCustomer = customerRepo.findById(request.getId());
      String notFoundMessage = String.format("customer for 'id' '%s' not found", request.getId());
      if (maybeCustomer.isEmpty()) {
        throw new NotFoundException(notFoundMessage);
      }
    } else {
      maybeCustomer = customerRepo.findByStellarAccountAndMemoAndMemoType(request.getAccount(), request.getMemo(), request.getMemoType());
      if (maybeCustomer.isEmpty()) {
        return createNewCustomerResponse(request.getType());
      }
    }
    return createExistingCustomerResponse(maybeCustomer.get());
  }

  /**
   * If the customer is found, update the customer. Otherwise, create and insert a new customer.
   *
   * @param request the PUT customer request.
   * @return the PUT Customer response.
   */
  public PutCustomerResponse upsertCustomer(PutCustomerRequest request) throws NotFoundException {
    Customer customer;
    if (request.getId() != null) {
      customer = getCustomerByRequestId(request.getId());
      updateCustomer(customer, request);
    } else {
      Optional<Customer> maybeCustomer = customerRepo.findByStellarAccountAndMemoAndMemoType(request.getAccount(), request.getMemo(), request.getMemoType());
      if (maybeCustomer.isEmpty()) {
        customer = createCustomer(request);
      } else {
        customer = maybeCustomer.get();
        updateCustomer(customer, request);
      }
    }
    return new PutCustomerResponse(customer.getId(), customer.getStatus(request.getType()));
  }

  public void delete(DeleteCustomerRequest request) throws NotFoundException {
    throw new RuntimeException("Not implemented");
  }

  public Customer getCustomerByRequestId(String id) throws NotFoundException {
    Optional<Customer> maybeCustomer = customerRepo.findById(id);
    String notFoundMessage = String.format("customer for 'id' '%s' not found", id);
    if (maybeCustomer.isEmpty()) {
      throw new NotFoundException(notFoundMessage);
    }
    return maybeCustomer.get();
  }

  public void updateCustomer(Customer customer, PutCustomerRequest request) {
    if (request.getFirstName() != null) {
      customer.setFirstName(request.getFirstName());
    }
    if (request.getLastName() != null) {
      customer.setFirstName(request.getLastName());
    }
    if (request.getEmailAddress() != null) {
      customer.setFirstName(request.getEmailAddress());
    }
    if (request.getType() == Customer.Type.sep31Receiver) {
      if (request.getBankAccountNumber() != null) {
        customer.setBankAccountNumber(request.getBankAccountNumber());
      }
      if (request.getBankNumber() != null) {
        customer.setBankRoutingNumber(request.getBankNumber());
      }
    }
    customer.setStatus(Customer.Status.ACCEPTED);
  }
}

package org.stellar.anchor.reference.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.ws.rs.NotFoundException;

import org.springframework.stereotype.Service;

import org.stellar.platform.apis.callbacks.requests.DeleteCustomerRequest;
import org.stellar.platform.apis.callbacks.requests.GetCustomerRequest;
import org.stellar.platform.apis.callbacks.requests.PutCustomerRequest;
import org.stellar.platform.apis.shared.Field;
import org.stellar.platform.apis.callbacks.responses.GetCustomerResponse;

import org.stellar.anchor.reference.model.Customer;
import org.stellar.anchor.reference.repo.CustomerRepo;
import org.stellar.platform.apis.callbacks.responses.PutCustomerResponse;
import org.stellar.platform.apis.shared.ProvidedField;

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
    return createExistingCustomerResponse(maybeCustomer.get(), request.getType());
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
    return new PutCustomerResponse(customer.getId(), getStatusForCustomer(customer, request.getType()));
  }

  public void delete(DeleteCustomerRequest request) throws NotFoundException {
    throw new RuntimeException("Not implemented");
  }

  private Customer getCustomerByRequestId(String id) throws NotFoundException {
    Optional<Customer> maybeCustomer = customerRepo.findById(id);
    String notFoundMessage = String.format("customer for 'id' '%s' not found", id);
    if (maybeCustomer.isEmpty()) {
      throw new NotFoundException(notFoundMessage);
    }
    return maybeCustomer.get();
  }

  private GetCustomerResponse createNewCustomerResponse(String type) {
    GetCustomerResponse response = new GetCustomerResponse();
    response.setStatus(Customer.Status.NEEDS_INFO);
    Map<String, Field> fields = getBasicFields();
    if (type.equals(Customer.Type.SEP31_RECEIVER.toString())) {
      fields.putAll(getSep31ReceiverFields());
    }
    response.setFields(fields);
    return response;
  }

  private GetCustomerResponse createExistingCustomerResponse(Customer customer, String type) {
    GetCustomerResponse response = new GetCustomerResponse();
    Map<String, ProvidedField> providedFields = new HashMap<String, ProvidedField>();
    Map<String, Field> fields = new HashMap<String, Field>();
    if (customer.getFirstName() != null) {
      providedFields.put("first_name", createFirstNameProvidedField());
    } else {
      fields.put("first_name", createFirstNameField());
    }
    if (customer.getLastName() != null) {
      providedFields.put("last_name", createLastNameProvidedField());
    } else {
      fields.put("last_name", createLastNameField());
    }
    if (customer.getEmail() != null) {
      providedFields.put("email_address", createEmailProvidedField());
    } else {
      fields.put("email_address", createEmailField());
    }
    if (type.equals(Customer.Type.SEP31_RECEIVER.toString())) {
      if (customer.getBankAccountNumber() != null) {
        providedFields.put("bank_account_number", createBankAccountNumberProvidedField());
      } else {
        fields.put("bank_account_number", createBankAccountNumberField());
      }
      if (customer.getBankRoutingNumber() != null) {
        providedFields.put("bank_number", createBankNumberProvidedField());
      } else {
        fields.put("bank_number", createBankNumberField());
      }
    }
    response.setFields(fields);
    response.setProvidedFields(providedFields);
    response.setStatus(getStatusForCustomer(customer, type));
    return response;
  }

  private Customer createCustomer(PutCustomerRequest request) {
    Customer customer = new Customer();
    customer.setId(UUID.randomUUID().toString());
    updateCustomer(customer, request);
    return customer;
  }

  private void updateCustomer(Customer customer, PutCustomerRequest request) {
    if (request.getFirstName() != null) {
      customer.setFirstName(request.getFirstName());
    }
    if (request.getLastName() != null) {
      customer.setFirstName(request.getLastName());
    }
    if (request.getEmailAddress() != null) {
      customer.setFirstName(request.getEmailAddress());
    }
    if (request.getType().equals(Customer.Type.SEP31_RECEIVER.toString())) {
      if (request.getBankAccountNumber() != null) {
        customer.setBankAccountNumber(request.getBankAccountNumber());
      }
      if (request.getBankNumber() != null) {
        customer.setBankRoutingNumber(request.getBankNumber());
      }
    }
    customerRepo.save(customer);
  }
}

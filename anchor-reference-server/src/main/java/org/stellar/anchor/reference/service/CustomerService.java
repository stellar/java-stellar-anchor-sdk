package org.stellar.anchor.reference.service;

import static org.stellar.anchor.reference.model.Customer.Status.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.stellar.anchor.exception.NotFoundException;
import org.stellar.anchor.reference.model.Customer;
import org.stellar.anchor.reference.repo.CustomerRepo;
import org.stellar.platform.apis.callbacks.requests.DeleteCustomerRequest;
import org.stellar.platform.apis.callbacks.requests.GetCustomerRequest;
import org.stellar.platform.apis.callbacks.requests.PutCustomerRequest;
import org.stellar.platform.apis.callbacks.responses.DeleteCustomerResponse;
import org.stellar.platform.apis.callbacks.responses.GetCustomerResponse;
import org.stellar.platform.apis.callbacks.responses.PutCustomerResponse;
import org.stellar.platform.apis.shared.Field;
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
      if (maybeCustomer.isEmpty()) {
        throw new NotFoundException(
            String.format("customer for 'id' '%s' not found", request.getId()));
      }
    } else {
      maybeCustomer =
          customerRepo.findByStellarAccountAndMemoAndMemoType(
              request.getAccount(), request.getMemo(), request.getMemoType());
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
      Optional<Customer> maybeCustomer =
          customerRepo.findByStellarAccountAndMemoAndMemoType(
              request.getAccount(), request.getMemo(), request.getMemoType());
      if (maybeCustomer.isEmpty()) {
        customer = createCustomer(request);
      } else {
        customer = maybeCustomer.get();
        updateCustomer(customer, request);
      }
    }
    PutCustomerResponse response = new PutCustomerResponse();
    response.setId(customer.getId());
    return response;
  }

  public DeleteCustomerResponse delete(DeleteCustomerRequest request) {
    customerRepo.deleteById(request.getId());
    DeleteCustomerResponse response = new DeleteCustomerResponse();
    response.setId(request.getId());
    return response;
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
    response.setStatus(Customer.Status.NEEDS_INFO.toString());
    Map<String, Field> fields = getBasicFields();
    // type can be null.
    if (Customer.Type.SEP31_RECEIVER.toString().equals(type)) {
      fields.putAll(getSep31ReceiverFields(type));
    }
    response.setFields(fields);
    return response;
  }

  private GetCustomerResponse createExistingCustomerResponse(Customer customer, String type) {
    GetCustomerResponse response = new GetCustomerResponse();
    Map<String, ProvidedField> providedFields = new HashMap<>();
    Map<String, Field> fields = new HashMap<>();
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
    if (Customer.Type.SEP31_RECEIVER.toString().equals(type)) {
      if (customer.getBankAccountNumber() != null) {
        providedFields.put("bank_account_number", createBankAccountNumberProvidedField());
      } else {
        fields.put("bank_account_number", createBankAccountNumberField(type));
      }
      if (customer.getBankRoutingNumber() != null) {
        providedFields.put("bank_number", createBankNumberProvidedField());
      } else {
        fields.put("bank_number", createBankNumberField(type));
      }
    }
    response.setId(customer.getId());
    response.setFields(fields);
    response.setProvidedFields(providedFields);
    Customer.Status status = (fields.size() > 0) ? NEEDS_INFO : ACCEPTED;
    response.setStatus(status.toString());
    return response;
  }

  private Customer createCustomer(PutCustomerRequest request) {
    Customer customer = new Customer();
    customer.setId(UUID.randomUUID().toString());
    updateCustomer(customer, request);
    return customer;
  }

  private void updateCustomer(Customer customer, PutCustomerRequest request) {
    customer.setStellarAccount(request.getAccount());
    customer.setMemo(request.getMemo());
    customer.setMemoType(request.getMemoType());

    if (request.getFirstName() != null) {
      customer.setFirstName(request.getFirstName());
    }
    if (request.getLastName() != null) {
      customer.setLastName(request.getLastName());
    }
    if (request.getEmailAddress() != null) {
      customer.setEmail(request.getEmailAddress());
    }
    if (Customer.Type.SEP31_RECEIVER.toString().equals(request.getType())) {
      if (request.getBankAccountNumber() != null) {
        customer.setBankAccountNumber(request.getBankAccountNumber());
      }
      if (request.getBankNumber() != null) {
        customer.setBankRoutingNumber(request.getBankNumber());
      }
    }
    customerRepo.save(customer);
  }

  public Map<String, Field> getBasicFields() {
    Map<String, Field> map = new HashMap<>();
    map.put("first_name", createFirstNameField());
    map.put("last_name", createLastNameField());
    map.put("email_address", createEmailField());
    return map;
  }

  public Map<String, Field> getSep31ReceiverFields(String type) {
    Map<String, Field> map = new HashMap<>();
    map.put("bank_account_number", createBankAccountNumberField(type));
    map.put("bank_number", createBankNumberField(type));
    return map;
  }

  public Field createFirstNameField() {
    Field field = new Field();
    field.setType("string");
    field.setDescription("first name of the customer");
    field.setOptional(false);
    return field;
  }

  public Field createLastNameField() {
    Field field = new Field();
    field.setType("string");
    field.setDescription("last name of the customer");
    field.setOptional(false);
    return field;
  }

  public Field createEmailField() {
    Field field = new Field();
    field.setType("string");
    field.setDescription("email of the customer");
    field.setOptional(false);
    return field;
  }

  public Field createBankAccountNumberField(String type) {
    Field field = new Field();
    field.setType("string");
    field.setDescription("bank account number of the customer");
    field.setOptional(!type.equals(Customer.Type.SEP31_RECEIVER.toString()));
    return field;
  }

  public Field createBankNumberField(String type) {
    Field field = new Field();
    field.setType("string");
    field.setDescription("bank routing number of the customer");
    field.setOptional(!type.equals(Customer.Type.SEP31_RECEIVER.toString()));
    return field;
  }

  public ProvidedField createFirstNameProvidedField() {
    ProvidedField field = new ProvidedField();
    field.setType("string");
    field.setDescription("first name of the customer");
    field.setStatus(Customer.Status.ACCEPTED.toString());
    return field;
  }

  public ProvidedField createLastNameProvidedField() {
    ProvidedField field = new ProvidedField();
    field.setType("string");
    field.setDescription("last name of the customer");
    field.setStatus(Customer.Status.ACCEPTED.toString());
    return field;
  }

  public ProvidedField createEmailProvidedField() {
    ProvidedField field = new ProvidedField();
    field.setType("string");
    field.setDescription("email of the customer");
    field.setStatus(Customer.Status.ACCEPTED.toString());
    return field;
  }

  public ProvidedField createBankAccountNumberProvidedField() {
    ProvidedField field = new ProvidedField();
    field.setType("string");
    field.setDescription("bank account of the customer");
    field.setStatus(Customer.Status.ACCEPTED.toString());
    return field;
  }

  public ProvidedField createBankNumberProvidedField() {
    ProvidedField field = new ProvidedField();
    field.setType("string");
    field.setDescription("bank routing number of the customer");
    field.setStatus(Customer.Status.ACCEPTED.toString());
    return field;
  }
}

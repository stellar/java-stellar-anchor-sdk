package org.stellar.anchor.reference.service;

import static org.stellar.anchor.reference.model.Customer.Status.ACCEPTED;
import static org.stellar.anchor.reference.model.Customer.Status.NEEDS_INFO;

import java.util.*;
import org.springframework.stereotype.Service;
import org.stellar.anchor.api.callback.GetCustomerRequest;
import org.stellar.anchor.api.callback.GetCustomerResponse;
import org.stellar.anchor.api.callback.PutCustomerRequest;
import org.stellar.anchor.api.callback.PutCustomerResponse;
import org.stellar.anchor.api.exception.NotFoundException;
import org.stellar.anchor.api.shared.CustomerField;
import org.stellar.anchor.api.shared.ProvidedCustomerField;
import org.stellar.anchor.reference.model.Customer;
import org.stellar.anchor.reference.repo.CustomerRepo;

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
      } else if (request.getType() != null) {
        if (!request.getType().equals("sep31-sender")
            && !request.getType().equals("sep31-receiver")) {
          throw new NotFoundException(
              String.format(
                  "customer for 'id' '%s' and 'type' '%s' not found",
                  request.getId(), request.getType()));
        }
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

  /**
   * ATTENTION: this function is used for testing purposes only.
   *
   * <p>This method is used to delete a customer's `clabe_number`, which would make its state change
   * to NEEDS_INFO if it's a receiving customer.
   *
   * @param customerId is the id of the customer whose `clabe_number` will be deleted.
   * @throws NotFoundException if the user was not found.
   */
  public void invalidateCustomerClabe(String customerId) throws NotFoundException {
    Optional<Customer> maybeCustomer = customerRepo.findById(customerId);
    if (maybeCustomer.isEmpty()) {
      throw new NotFoundException(String.format("customer for 'id' '%s' not found", customerId));
    }

    Customer customer = maybeCustomer.get();
    customer.setClabeNumber(null);
    customerRepo.save(customer);
  }

  public void delete(String customerId) {
    customerRepo.deleteById(customerId);
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
    Map<String, CustomerField> fields = getBasicFields();
    // type can be null.
    if (Customer.Type.SEP31_RECEIVER.toString().equals(type)) {
      fields.putAll(getSep31ReceiverFields(type));
    }
    response.setFields(fields);
    return response;
  }

  private GetCustomerResponse createExistingCustomerResponse(Customer customer, String type) {
    GetCustomerResponse response = new GetCustomerResponse();
    Map<String, ProvidedCustomerField> providedFields = new HashMap<>();
    Map<String, CustomerField> fields = new HashMap<>();
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
      if (customer.getBankAccountType() != null) {
        providedFields.put("bank_account_type", createBankAccountTypeProvidedField());
      } else {
        fields.put("bank_account_type", createBankAccountTypeField(type));
      }
      if (customer.getBankRoutingNumber() != null) {
        providedFields.put("bank_number", createBankNumberProvidedField());
      } else {
        fields.put("bank_number", createBankNumberField(type));
      }
      if (customer.getClabeNumber() != null) {
        providedFields.put("clabe_number", createClabeNumberProvidedField());
      } else {
        fields.put("clabe_number", createClabeNumberField(type));
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
    if (request.getBankAccountNumber() != null) {
      customer.setBankAccountNumber(request.getBankAccountNumber());
    }
    if (request.getBankAccountType() != null) {
      customer.setBankAccountType(request.getBankAccountType());
    }
    if (request.getBankNumber() != null) {
      customer.setBankRoutingNumber(request.getBankNumber());
    }
    if (request.getClabeNumber() != null) {
      customer.setClabeNumber(request.getClabeNumber());
    }
    customerRepo.save(customer);
  }

  public Map<String, CustomerField> getBasicFields() {
    Map<String, CustomerField> map = new HashMap<>();
    map.put("first_name", createFirstNameField());
    map.put("last_name", createLastNameField());
    map.put("email_address", createEmailField());
    return map;
  }

  public Map<String, CustomerField> getSep31ReceiverFields(String type) {
    Map<String, CustomerField> map = new HashMap<>();
    map.put("bank_account_number", createBankAccountNumberField(type));
    map.put("bank_account_type", createBankAccountTypeField(type));
    map.put("bank_number", createBankNumberField(type));
    map.put("clabe_number", createClabeNumberField(type));
    return map;
  }

  public CustomerField createFirstNameField() {
    CustomerField field = new CustomerField();
    field.setType("string");
    field.setDescription("first name of the customer");
    field.setOptional(false);
    return field;
  }

  public CustomerField createLastNameField() {
    CustomerField field = new CustomerField();
    field.setType("string");
    field.setDescription("last name of the customer");
    field.setOptional(false);
    return field;
  }

  public CustomerField createEmailField() {
    CustomerField field = new CustomerField();
    field.setType("string");
    field.setDescription("email of the customer");
    field.setOptional(false);
    return field;
  }

  public CustomerField createBankAccountNumberField(String type) {
    CustomerField field = new CustomerField();
    field.setType("string");
    field.setDescription("bank account number of the customer");
    field.setOptional(!type.equals(Customer.Type.SEP31_RECEIVER.toString()));
    return field;
  }

  public CustomerField createBankAccountTypeField(String type) {
    CustomerField field = new CustomerField();
    field.setType("string");
    field.setDescription("bank account type of the customer");
    field.setChoices(Arrays.asList("checking", "savings"));
    field.setOptional(!type.equals(Customer.Type.SEP31_RECEIVER.toString()));
    return field;
  }

  public CustomerField createBankNumberField(String type) {
    CustomerField field = new CustomerField();
    field.setType("string");
    field.setDescription("bank routing number of the customer");
    field.setOptional(!type.equals(Customer.Type.SEP31_RECEIVER.toString()));
    return field;
  }

  public CustomerField createClabeNumberField(String customerType) {
    CustomerField field = new CustomerField();
    field.setType("string");
    field.setDescription("Bank account number for Mexico");
    field.setOptional(!customerType.equals(Customer.Type.SEP31_RECEIVER.toString()));
    return field;
  }

  public ProvidedCustomerField createFirstNameProvidedField() {
    ProvidedCustomerField field = new ProvidedCustomerField();
    field.setType("string");
    field.setDescription("first name of the customer");
    field.setStatus(Customer.Status.ACCEPTED.toString());
    return field;
  }

  public ProvidedCustomerField createLastNameProvidedField() {
    ProvidedCustomerField field = new ProvidedCustomerField();
    field.setType("string");
    field.setDescription("last name of the customer");
    field.setStatus(Customer.Status.ACCEPTED.toString());
    return field;
  }

  public ProvidedCustomerField createEmailProvidedField() {
    ProvidedCustomerField field = new ProvidedCustomerField();
    field.setType("string");
    field.setDescription("email of the customer");
    field.setStatus(Customer.Status.ACCEPTED.toString());
    return field;
  }

  public ProvidedCustomerField createBankAccountNumberProvidedField() {
    ProvidedCustomerField field = new ProvidedCustomerField();
    field.setType("string");
    field.setDescription("bank account of the customer");
    field.setStatus(Customer.Status.ACCEPTED.toString());
    return field;
  }

  public ProvidedCustomerField createBankAccountTypeProvidedField() {
    ProvidedCustomerField field = new ProvidedCustomerField();
    field.setType("string");
    field.setDescription("bank account type of the customer");
    field.setStatus(Customer.Status.ACCEPTED.toString());
    field.setChoices(Arrays.asList("checking", "savings"));
    return field;
  }

  public ProvidedCustomerField createBankNumberProvidedField() {
    ProvidedCustomerField field = new ProvidedCustomerField();
    field.setType("string");
    field.setDescription("bank routing number of the customer");
    field.setStatus(Customer.Status.ACCEPTED.toString());
    return field;
  }

  public ProvidedCustomerField createClabeNumberProvidedField() {
    ProvidedCustomerField field = new ProvidedCustomerField();
    field.setType("string");
    field.setDescription("bank account number for Mexico");
    field.setStatus(Customer.Status.ACCEPTED.toString());
    return field;
  }
}

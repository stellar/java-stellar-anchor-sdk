package org.stellar.anchor.reference.controller;

import org.springframework.web.bind.annotation.*;
import org.stellar.anchor.exception.NotFoundException;
import org.stellar.anchor.reference.config.AppSettings;
import org.stellar.anchor.reference.service.CustomerService;
import org.stellar.platform.apis.callbacks.requests.DeleteCustomerRequest;
import org.stellar.platform.apis.callbacks.requests.GetCustomerRequest;
import org.stellar.platform.apis.callbacks.requests.PutCustomerRequest;
import org.stellar.platform.apis.callbacks.responses.DeleteCustomerResponse;
import org.stellar.platform.apis.callbacks.responses.GetCustomerResponse;
import org.stellar.platform.apis.callbacks.responses.PutCustomerResponse;

@RestController
public class CustomerController {
  private final CustomerService customerService;

  public CustomerController(AppSettings appSettings, CustomerService customerService) {
    this.customerService = customerService;
  }

  /** Gets a customer */
  @RequestMapping(
      value = "/customer",
      method = {RequestMethod.GET})
  public GetCustomerResponse getCustomer(GetCustomerRequest getCustomerRequest)
      throws NotFoundException {
    return customerService.getCustomer(getCustomerRequest);
  }

  /** Puts a customer */
  @RequestMapping(
      value = "/customer",
      method = {RequestMethod.PUT})
  public PutCustomerResponse putCustomer(@RequestBody PutCustomerRequest request)
      throws NotFoundException {
    return customerService.upsertCustomer(request);
  }

  /** Delete a customer. */
  @RequestMapping(
      value = "/customer/{id}",
      method = {RequestMethod.DELETE})
  public DeleteCustomerResponse deleteCustomer(@PathVariable String id) throws NotFoundException {
    DeleteCustomerRequest request = new DeleteCustomerRequest();
    request.setId(id);
    return customerService.delete(request);
  }
}

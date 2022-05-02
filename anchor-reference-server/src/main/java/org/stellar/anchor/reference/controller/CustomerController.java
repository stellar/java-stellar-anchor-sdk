package org.stellar.anchor.reference.controller;

import org.springframework.web.bind.annotation.*;
import org.stellar.anchor.api.callback.GetCustomerRequest;
import org.stellar.anchor.api.callback.GetCustomerResponse;
import org.stellar.anchor.api.callback.PutCustomerRequest;
import org.stellar.anchor.api.callback.PutCustomerResponse;
import org.stellar.anchor.api.exception.NotFoundException;
import org.stellar.anchor.reference.service.CustomerService;

@RestController
public class CustomerController {
  private final CustomerService customerService;

  public CustomerController(CustomerService customerService) {
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
  public void deleteCustomer(@PathVariable String id) {
    customerService.delete(id);
  }
}

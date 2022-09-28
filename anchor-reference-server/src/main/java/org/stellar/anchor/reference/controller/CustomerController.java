package org.stellar.anchor.reference.controller;

import org.springframework.http.MediaType;
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
      consumes = {MediaType.APPLICATION_JSON_VALUE},
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

  /**
   * ATTENTION: this function is used for testing purposes only.
   *
   * <p>This endpoint is used to delete a customer's `clabe_number`, which would make its state
   * change to NEEDS_INFO if it's a receiving customer.
   */
  @RequestMapping(
      value = "/invalidate_clabe/{id}",
      method = {RequestMethod.GET})
  public void invalidateCustomerClabe(@PathVariable String id) throws NotFoundException {
    customerService.invalidateCustomerClabe(id);
  }
}

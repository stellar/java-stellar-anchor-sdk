package org.stellar.anchor.reference.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.stellar.anchor.exception.SepNotFoundException;
import org.stellar.anchor.integration.customer.*;
import org.stellar.anchor.reference.config.AppSettings;
import org.stellar.anchor.reference.service.CustomerService;

@RestController
public class CustomerController {
  private final AppSettings appSettings;
  private final CustomerService customerService;

  public CustomerController(AppSettings appSettings, CustomerService customerService) {
    this.appSettings = appSettings;
    this.customerService = customerService;
  }

  /**
   * Gets a customer
   *
   * @return list of services available.
   */
  @RequestMapping(
      value = "/customers",
      method = {RequestMethod.GET})
  public GetCustomerResponse getCustomer(@RequestParam(required = false) String id)
      throws SepNotFoundException {
    GetCustomerRequest request = new GetCustomerRequest();
    request.setId(id);
    return customerService.getCustomer(request);
  }

  /**
   * Puts a customer
   *
   * @return list of services available.
   */
  @RequestMapping(
      value = "/customers",
      method = {RequestMethod.PUT})
  public PutCustomerResponse putCustomer(@RequestParam PutCustomerRequest request)
      throws SepNotFoundException {
    return customerService.upsertCustomer(request);
  }

  /**
   * Delete a customer.
   *
   * @param request Delete a customer request.
   * @throws SepNotFoundException If the user is not found, an exception is thrown.
   */
  @RequestMapping(
      value = "/customers",
      method = {RequestMethod.DELETE})
  public void deleteCustomer(@RequestParam DeleteCustomerRequest request)
      throws SepNotFoundException {
    customerService.delete(request);
  }
}

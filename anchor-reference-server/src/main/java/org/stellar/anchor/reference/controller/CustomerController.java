package org.stellar.anchor.reference.controller;

import javax.ws.rs.NotFoundException;
import org.springframework.web.bind.annotation.*;
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
  private final AppSettings appSettings;
  private final CustomerService customerService;

  public CustomerController(AppSettings appSettings, CustomerService customerService) {
    this.appSettings = appSettings;
    this.customerService = customerService;
  }

  /** Gets a customer */
  @RequestMapping(
      value = "/customer",
      method = {RequestMethod.GET})
  public GetCustomerResponse getCustomer(
      @RequestParam(required = false) String id,
      @RequestParam(required = false) String account,
      @RequestParam(required = false) String memo,
      @RequestParam(required = false, name = "memo_type") String memoType,
      @RequestParam(required = false) String type)
      throws NotFoundException {
    GetCustomerRequest request =
        GetCustomerRequest.builder()
            .id(id)
            .account(account)
            .memo(memo)
            .memoType(memoType)
            .type(type)
            .build();
    return customerService.getCustomer(request);
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

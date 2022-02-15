package org.stellar.anchor.server.controller;

import static org.stellar.anchor.util.Log.debugF;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.stellar.anchor.paymentservice.PaymentGateway;

@RestController
public class PaymentGatewayController {
  private PaymentGateway paymentGateway;

  public PaymentGatewayController(PaymentGateway paymentGateway) {
    this.paymentGateway = paymentGateway;
  }

  /**
   * Example endpoint. TODO: To be replaced.
   *
   * @return list of services available.
   */
  @CrossOrigin(origins = "*")
  @RequestMapping(
      value = "/payment/services",
      method = {RequestMethod.GET})
  public String getServices() {
    debugF("/services");
    return paymentGateway.getServices();
  }
}

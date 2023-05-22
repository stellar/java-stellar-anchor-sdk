package org.stellar.anchor.platform.controller.custody;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.stellar.anchor.api.custody.GenerateDepositAddressResponse;
import org.stellar.anchor.api.exception.FireblocksException;
import org.stellar.anchor.api.exception.InvalidConfigException;
import org.stellar.anchor.platform.custody.PaymentService;

@RestController
public class PaymentController {

  private final PaymentService paymentService;

  public PaymentController(PaymentService paymentService) {
    this.paymentService = paymentService;
  }

  @CrossOrigin(origins = "*")
  @ResponseStatus(code = HttpStatus.OK)
  @RequestMapping(
      value = "/transactions/payments/assets/{assetId}/address",
      method = {RequestMethod.POST})
  public GenerateDepositAddressResponse generateDepositAddress(@PathVariable String assetId)
      throws FireblocksException, InvalidConfigException {
    return paymentService.generateDepositAddress(assetId);
  }
}

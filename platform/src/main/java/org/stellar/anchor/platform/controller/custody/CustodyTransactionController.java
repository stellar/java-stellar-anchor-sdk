package org.stellar.anchor.platform.controller.custody;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.stellar.anchor.api.custody.CreateCustodyTransactionRequest;
import org.stellar.anchor.platform.service.CustodyTransactionService;

@RestController
public class CustodyTransactionController {

  private final CustodyTransactionService custodyTransactionService;

  public CustodyTransactionController(CustodyTransactionService custodyTransactionService) {
    this.custodyTransactionService = custodyTransactionService;
  }

  @CrossOrigin(origins = "*")
  @ResponseStatus(code = HttpStatus.OK)
  @RequestMapping(
      value = "/transactions/custody",
      method = {RequestMethod.POST})
  public void createCustodyTransaction(@RequestBody CreateCustodyTransactionRequest request) {
    custodyTransactionService.create(request);
  }
}

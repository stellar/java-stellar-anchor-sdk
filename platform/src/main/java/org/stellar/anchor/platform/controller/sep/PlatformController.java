package org.stellar.anchor.platform.controller.sep;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.stellar.anchor.api.custody.CreateTransactionPaymentResponse;
import org.stellar.anchor.api.exception.AnchorException;
import org.stellar.anchor.api.exception.NotFoundException;
import org.stellar.anchor.api.platform.GetTransactionResponse;
import org.stellar.anchor.api.platform.PatchTransactionsRequest;
import org.stellar.anchor.api.platform.PatchTransactionsResponse;
import org.stellar.anchor.platform.service.TransactionService;

@RestController
public class PlatformController {

  private final TransactionService transactionService;

  PlatformController(TransactionService transactionService) {
    this.transactionService = transactionService;
  }

  @CrossOrigin(origins = "*")
  @ResponseStatus(code = HttpStatus.OK)
  @RequestMapping(
      value = "/transactions/{id}",
      method = {RequestMethod.GET})
  public GetTransactionResponse getTransaction(@PathVariable(name = "id") String txnId)
      throws AnchorException {
    return transactionService.getTransactionResponse(txnId);
  }

  @CrossOrigin(origins = "*")
  @RequestMapping(
      value = "/transactions/{id}/payments",
      method = {RequestMethod.POST},
      consumes = {MediaType.APPLICATION_JSON_VALUE})
  public CreateTransactionPaymentResponse createCustodyTransactionPayment(
      @PathVariable(name = "id") String txnId, @RequestBody String requestBody)
      throws AnchorException {
    return transactionService.createCustodyTransactionPayment(txnId, requestBody);
  }

  @CrossOrigin(origins = "*")
  @ResponseStatus(code = HttpStatus.OK)
  @RequestMapping(
      value = "/transactions",
      consumes = {MediaType.APPLICATION_JSON_VALUE},
      method = {RequestMethod.PATCH})
  public PatchTransactionsResponse patchTransactions(@RequestBody PatchTransactionsRequest request)
      throws AnchorException {
    return transactionService.patchTransactions(request);
  }

  @CrossOrigin(origins = "*")
  @ResponseStatus(code = HttpStatus.OK)
  @RequestMapping(
      value = "/transactions",
      method = {RequestMethod.GET})
  public GetTransactionResponse getTransactions() throws AnchorException {
    throw new NotFoundException("Not implemented");
  }
}

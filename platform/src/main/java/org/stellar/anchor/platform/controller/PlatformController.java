package org.stellar.anchor.platform.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.stellar.anchor.api.exception.AnchorException;
import org.stellar.anchor.api.exception.NotFoundException;
import org.stellar.anchor.api.platform.GetTransactionResponse;
import org.stellar.anchor.api.platform.PatchTransactionsRequest;
import org.stellar.anchor.api.platform.PatchTransactionsResponse;
import org.stellar.anchor.platform.service.TransactionService;
import org.stellar.anchor.sep31.Sep31Service;

@RestController
public class PlatformController {

  private final TransactionService transactionService;
  private final Sep31Service sep31Service;

  PlatformController(TransactionService transactionService, Sep31Service sep31Service) {
    this.transactionService = transactionService;
    this.sep31Service = sep31Service;
  }

  @CrossOrigin(origins = "*")
  @ResponseStatus(code = HttpStatus.OK)
  @RequestMapping(
      value = "/transactions/{id}",
      method = {RequestMethod.GET})
  public GetTransactionResponse getTransaction(@PathVariable(name = "id") String txnId)
      throws AnchorException {
    return transactionService.getTransaction(txnId);
  }

  @CrossOrigin(origins = "*")
  @ResponseStatus(code = HttpStatus.OK)
  @RequestMapping(
      value = "/transactions",
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
  public GetTransactionResponse getTransactions(@PathVariable(name = "id") String txnId)
      throws AnchorException {
    throw new NotFoundException("Not implemented");
  }
}

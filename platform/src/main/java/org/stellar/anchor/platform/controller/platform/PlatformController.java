package org.stellar.anchor.platform.controller.platform;

import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.stellar.anchor.api.exception.AnchorException;
import org.stellar.anchor.api.platform.*;
import org.stellar.anchor.api.sep.SepTransactionStatus;
import org.stellar.anchor.apiclient.TransactionsOrderBy;
import org.stellar.anchor.apiclient.TransactionsSeps;
import org.stellar.anchor.platform.service.TransactionService;
import org.stellar.anchor.util.TransactionsParams;

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
    return transactionService.findTransaction(txnId);
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
  public GetTransactionsResponse getTransactions(
      @RequestParam(value = "sep") TransactionsSeps sep,
      @RequestParam(required = false, value = "order_by", defaultValue = "created_at")
          TransactionsOrderBy order_by,
      @RequestParam(required = false, value = "order", defaultValue = "asc") Sort.Direction order,
      @RequestParam(required = false, value = "statuses") List<SepTransactionStatus> statuses,
      @RequestParam(required = false, value = "page_number", defaultValue = "0") Integer pageNumber,
      @RequestParam(required = false, value = "page_size", defaultValue = "20") Integer pageSize)
      throws AnchorException {
    TransactionsParams params =
        new TransactionsParams(order_by, order, statuses, pageNumber, pageSize);
    return transactionService.findTransactions(sep, params);
  }
}

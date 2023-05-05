package org.stellar.anchor.platform.controller.sep;

import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.stellar.anchor.api.exception.AnchorException;
import org.stellar.anchor.api.platform.*;
import org.stellar.anchor.api.sep.SepTransactionStatus;
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
      @RequestParam(required = false, value = "page_size", defaultValue = "20") Integer pageSize,
      @RequestParam(required = false, value = "pageNumber", defaultValue = "0") Integer pageNumber)
      throws AnchorException {
    return transactionService.getTransactionsResponse(
        sep, order_by, order, statuses, pageSize, pageNumber);
  }
}

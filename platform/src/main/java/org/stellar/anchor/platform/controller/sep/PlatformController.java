package org.stellar.anchor.platform.controller.sep;

import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME;

import java.time.Instant;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.stellar.anchor.api.exception.AnchorException;
import org.stellar.anchor.api.platform.*;
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
      @RequestParam(required = false, value = "requestTo") @DateTimeFormat(iso = DATE_TIME)
          Instant requestTo,
      @RequestParam(required = false, value = "requestFrom") @DateTimeFormat(iso = DATE_TIME)
          Instant requestFrom,
      @RequestParam(required = false, value = "order_by", defaultValue = "created_at")
          TransactionsOrderBy order_by,
      @RequestParam(required = false, value = "limit", defaultValue = "200") Integer limit,
      @RequestParam(required = false, value = "offset", defaultValue = "0") Integer offset)
      throws AnchorException {
    Instant to = requestTo == null ? Instant.now() : requestTo;
    Instant from = requestFrom == null ? Instant.EPOCH : requestFrom;

    return transactionService.getTransactionsResponse(sep, to, from, order_by, limit, offset);
  }
}

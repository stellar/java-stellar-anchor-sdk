package org.stellar.anchor.platform.controller.platform;

import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.stellar.anchor.api.custody.CreateTransactionPaymentResponse;
import org.stellar.anchor.api.exception.AnchorException;
import org.stellar.anchor.api.platform.GetTransactionResponse;
import org.stellar.anchor.api.platform.GetTransactionsResponse;
import org.stellar.anchor.api.platform.PatchTransactionsRequest;
import org.stellar.anchor.api.platform.PatchTransactionsResponse;
import org.stellar.anchor.api.sep.SepTransactionStatus;
import org.stellar.anchor.apiclient.TransactionsOrderBy;
import org.stellar.anchor.apiclient.TransactionsSeps;
import org.stellar.anchor.custody.CustodyService;
import org.stellar.anchor.platform.service.TransactionService;
import org.stellar.anchor.util.TransactionsParams;

@RestController
public class PlatformController {

  private final TransactionService transactionService;
  private final CustodyService custodyService;

  PlatformController(TransactionService transactionService, CustodyService custodyService) {
    this.transactionService = transactionService;
    this.custodyService = custodyService;
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
    return custodyService.createTransactionPayment(txnId, requestBody);
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
    return transactionService.getTransactionsResponse(sep, params);
  }
}

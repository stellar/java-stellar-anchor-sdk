package org.stellar.anchor.platform.controller.sep;

import static org.stellar.anchor.platform.controller.sep.Sep10Helper.getSep10Token;
import static org.stellar.anchor.util.Log.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.stellar.anchor.api.exception.*;
import org.stellar.anchor.api.sep.SepExceptionResponse;
import org.stellar.anchor.api.sep.sep24.*;
import org.stellar.anchor.auth.Sep10Jwt;
import org.stellar.anchor.platform.condition.ConditionalOnAllSepsEnabled;
import org.stellar.anchor.sep24.Sep24Service;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/sep24")
@ConditionalOnAllSepsEnabled(seps = {"sep24"})
public class Sep24Controller {
  private final Sep24Service sep24Service;

  Sep24Controller(Sep24Service sep24Service) {
    this.sep24Service = sep24Service;
  }

  @CrossOrigin(origins = "*")
  @RequestMapping(
      value = "/info",
      method = {RequestMethod.GET})
  public InfoResponse getInfo() {
    debug("/info");
    return sep24Service.getInfo();
  }

  @CrossOrigin(origins = "*")
  @RequestMapping(
      value = "/transactions/deposit/interactive",
      consumes = {MediaType.APPLICATION_JSON_VALUE},
      method = {RequestMethod.POST})
  public InteractiveTransactionResponse deposit(
      HttpServletRequest request, @RequestBody HashMap<String, String> requestData)
      throws SepException, MalformedURLException, URISyntaxException, EventPublishException {
    debug("/deposit", requestData);
    Sep10Jwt token = getSep10Token(request);
    InteractiveTransactionResponse itr = sep24Service.deposit(token, requestData);
    info("interactive redirection:", itr);
    return itr;
  }

  @CrossOrigin(origins = "*")
  @RequestMapping(
      value = "/transactions/deposit/interactive",
      method = {RequestMethod.POST},
      consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
  public InteractiveTransactionResponse depositAllType(HttpServletRequest request)
      throws SepException, MalformedURLException, URISyntaxException, EventPublishException {
    HashMap<String, String> requestData = new HashMap<>();
    for (Map.Entry<String, String[]> entry : request.getParameterMap().entrySet()) {
      requestData.put(entry.getKey(), entry.getValue()[0]);
    }

    return deposit(request, requestData);
  }

  @CrossOrigin(origins = "*")
  @RequestMapping(
      value = "/transactions/withdraw/interactive",
      consumes = {MediaType.APPLICATION_JSON_VALUE},
      method = {RequestMethod.POST})
  public InteractiveTransactionResponse withdraw(
      HttpServletRequest request, @RequestBody HashMap<String, String> requestData)
      throws SepException, MalformedURLException, URISyntaxException, EventPublishException {
    debug("/withdraw", requestData);
    Sep10Jwt token = getSep10Token(request);
    InteractiveTransactionResponse itr = sep24Service.withdraw(token, requestData);
    info("interactive redirection:", itr);
    return itr;
  }

  @CrossOrigin(origins = "*")
  @RequestMapping(
      value = "/transactions/withdraw/interactive",
      method = {RequestMethod.POST},
      consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
  public InteractiveTransactionResponse withdrawAllType(HttpServletRequest request)
      throws SepException, MalformedURLException, URISyntaxException, EventPublishException {
    HashMap<String, String> requestData = new HashMap<>();
    for (Map.Entry<String, String[]> entry : request.getParameterMap().entrySet()) {
      requestData.put(entry.getKey(), entry.getValue()[0]);
    }

    return withdraw(request, requestData);
  }

  @CrossOrigin(origins = "*")
  @RequestMapping(
      value = "/transactions",
      consumes = {MediaType.APPLICATION_JSON_VALUE},
      method = {RequestMethod.GET})
  public GetTransactionsResponse getTransactions(
      HttpServletRequest request, @RequestBody GetTransactionsRequest tr)
      throws SepException, MalformedURLException, URISyntaxException {
    debug("/transactions", tr);
    Sep10Jwt token = getSep10Token(request);
    return sep24Service.findTransactions(token, tr);
  }

  @CrossOrigin(origins = "*")
  @RequestMapping(
      value = "/transactions",
      consumes = {MediaType.ALL_VALUE},
      method = {RequestMethod.GET})
  public GetTransactionsResponse getTransactions(
      HttpServletRequest request,
      @RequestParam(value = "asset_code") String assetCode,
      @RequestParam(required = false, value = "kind") String kind,
      @RequestParam(required = false, value = "limit") Integer limit,
      @RequestParam(required = false, value = "paging_id") String pagingId,
      @RequestParam(required = false, value = "no_older_than") String noOlderThan,
      @RequestParam(required = false, value = "lang") String lang)
      throws MalformedURLException, URISyntaxException, SepException {
    debugF(
        "/transactions asset_code={} kind={} limit={} no_older_than={} paging_id={}",
        assetCode,
        kind,
        limit,
        noOlderThan,
        pagingId);
    GetTransactionsRequest gtr =
        GetTransactionsRequest.of(assetCode, kind, limit, noOlderThan, pagingId, lang);
    return getTransactions(request, gtr);
  }

  @CrossOrigin(origins = "*")
  @RequestMapping(
      value = "/transaction",
      consumes = {MediaType.APPLICATION_JSON_VALUE},
      method = {RequestMethod.GET})
  public Sep24GetTransactionResponse getTransaction(
      HttpServletRequest request, @RequestBody(required = false) GetTransactionRequest tr)
      throws SepException, IOException, URISyntaxException {
    debug("/transaction", tr);
    Sep10Jwt token = getSep10Token(request);

    return sep24Service.findTransaction(token, tr);
  }

  @CrossOrigin(origins = "*")
  @RequestMapping(
      value = "/transaction",
      consumes = {MediaType.ALL_VALUE},
      method = {RequestMethod.GET})
  public Sep24GetTransactionResponse getTransaction(
      HttpServletRequest request,
      @RequestParam(required = false, value = "id") String id,
      @RequestParam(required = false, value = "external_transaction_id")
          String externalTransactionId,
      @RequestParam(required = false, value = "stellar_transaction_id") String stellarTransactionId,
      @RequestParam(required = false, value = "lang") String lang)
      throws SepException, IOException, URISyntaxException {
    debugF(
        "/transaction id={} external_transaction_id={} stellar_transaction_id={}",
        id,
        externalTransactionId,
        stellarTransactionId);
    GetTransactionRequest tr =
        new GetTransactionRequest(id, stellarTransactionId, externalTransactionId, lang);
    return getTransaction(request, tr);
  }

  @ExceptionHandler({MethodArgumentNotValidException.class, SepValidationException.class})
  @ResponseStatus(value = HttpStatus.BAD_REQUEST)
  SepExceptionResponse handleValidation(Exception ex) {
    return new SepExceptionResponse(ex.getMessage());
  }

  @ExceptionHandler({SepNotFoundException.class})
  @ResponseStatus(value = HttpStatus.NOT_FOUND)
  SepExceptionResponse handleNotFound(Exception ex) {
    return new SepExceptionResponse(ex.getMessage());
  }
}

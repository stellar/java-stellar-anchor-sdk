package org.stellar.anchor.platform.controller;

import static org.stellar.anchor.platform.controller.Sep10Helper.getSep10Token;
import static org.stellar.anchor.util.Log.debugF;
import static org.stellar.anchor.util.Log.errorEx;

import javax.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.stellar.anchor.api.exception.AnchorException;
import org.stellar.anchor.api.exception.Sep31CustomerInfoNeededException;
import org.stellar.anchor.api.exception.Sep31MissingFieldException;
import org.stellar.anchor.api.sep.AssetInfo.Sep31TxnFieldSpecs;
import org.stellar.anchor.api.sep.sep31.*;
import org.stellar.anchor.auth.JwtToken;
import org.stellar.anchor.platform.condition.ConditionalOnAllSepsEnabled;
import org.stellar.anchor.sep31.Sep31Service;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("sep31")
@ConditionalOnAllSepsEnabled(seps = {"sep31"})
public class Sep31Controller {
  private final Sep31Service sep31Service;

  public Sep31Controller(Sep31Service sep31Service) {
    this.sep31Service = sep31Service;
  }

  @CrossOrigin(origins = "*")
  @RequestMapping(
      value = "/info",
      method = {RequestMethod.GET})
  public Sep31InfoResponse getInfo() {
    debugF("GET /info");
    return sep31Service.getInfo();
  }

  @CrossOrigin(origins = "*")
  @ResponseStatus(code = HttpStatus.CREATED)
  @RequestMapping(
      value = "/transactions",
      consumes = {MediaType.APPLICATION_JSON_VALUE},
      method = {RequestMethod.POST})
  public Sep31PostTransactionResponse postTransaction(
      HttpServletRequest servletRequest, @RequestBody Sep31PostTransactionRequest request)
      throws AnchorException {
    JwtToken jwtToken = getSep10Token(servletRequest);
    debugF("POST /transactions request={}", request);
    return sep31Service.postTransaction(jwtToken, request);
  }

  @CrossOrigin(origins = "*")
  @ResponseStatus(code = HttpStatus.OK)
  @RequestMapping(
      value = "/transactions/{id}",
      method = {RequestMethod.GET})
  public Sep31GetTransactionResponse getTransaction(
      HttpServletRequest servletRequest, @PathVariable(name = "id") String txnId)
      throws AnchorException {
    JwtToken jwtToken = getSep10Token(servletRequest);
    debugF("GET /transactions id={}", txnId);
    return sep31Service.getTransaction(txnId);
  }

  @CrossOrigin(origins = "*")
  @ResponseStatus(code = HttpStatus.OK)
  @RequestMapping(
      value = "/transactions/{id}",
      consumes = {MediaType.APPLICATION_JSON_VALUE},
      method = {RequestMethod.PATCH})
  public Sep31GetTransactionResponse patchTransaction(
      HttpServletRequest servletRequest,
      @PathVariable(name = "id") String txnId,
      @RequestBody Sep31PatchTransactionRequest request)
      throws AnchorException {
    JwtToken jwtToken = getSep10Token(servletRequest);
    debugF("PATCH /transactions id={} request={}", txnId, request);
    return sep31Service.patchTransaction(request);
  }

  @ExceptionHandler(Sep31MissingFieldException.class)
  @ResponseStatus(value = HttpStatus.BAD_REQUEST)
  public Sep31MissingFieldResponse handleMissingField(Sep31MissingFieldException smfex) {
    errorEx(smfex);
    return Sep31MissingFieldResponse.from(smfex);
  }

  @ExceptionHandler(Sep31CustomerInfoNeededException.class)
  @ResponseStatus(value = HttpStatus.BAD_REQUEST)
  public Sep31CustomerInfoNeededResponse handleCustomerInfoNeeded(
      Sep31CustomerInfoNeededException scinex) {
    errorEx(scinex);
    return new Sep31CustomerInfoNeededResponse(scinex.getType());
  }

  public static class Sep31MissingFieldResponse {
    String error;
    Sep31TxnFieldSpecs fields;

    public static Sep31MissingFieldResponse from(Sep31MissingFieldException exception) {
      Sep31MissingFieldResponse instance = new Sep31MissingFieldResponse();
      instance.error = "transaction_info_needed";
      instance.fields = exception.getMissingFields();

      return instance;
    }
  }

  private class Sep31CustomerInfoNeededResponse {
    String error;
    String type;

    public Sep31CustomerInfoNeededResponse(String type) {
      this.error = "customer_info_needed";
      this.type = type;
    }
  }
}

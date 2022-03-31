package org.stellar.anchor.platform.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.stellar.anchor.asset.AssetInfo.Sep31TxnFields;
import org.stellar.anchor.dto.sep31.Sep31InfoResponse;
import org.stellar.anchor.dto.sep31.Sep31PostTransactionRequest;
import org.stellar.anchor.dto.sep31.Sep31PostTransactionResponse;
import org.stellar.anchor.exception.AnchorException;
import org.stellar.anchor.sep10.JwtToken;
import org.stellar.anchor.sep31.Sep31Service;
import org.stellar.anchor.sep31.Sep31Service.Sep31CustomerInfoNeededException;
import org.stellar.anchor.sep31.Sep31Service.Sep31MissingFieldException;

import javax.servlet.http.HttpServletRequest;

import static org.stellar.anchor.platform.controller.Sep10Helper.getSep10Token;
import static org.stellar.anchor.util.Log.errorEx;

@RestController
@RequestMapping("sep31")
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
    return sep31Service.getInfo();
  }

  @CrossOrigin(origins = "*")
  @ResponseStatus(code = HttpStatus.CREATED)
  @RequestMapping(
      value = "/quote",
      consumes = {MediaType.APPLICATION_JSON_VALUE},
      method = {RequestMethod.POST})
  public Sep31PostTransactionResponse postTransaction(
      HttpServletRequest servletRequest, Sep31PostTransactionRequest request)
      throws AnchorException {
    JwtToken jwtToken = getSep10Token(servletRequest);
    return sep31Service.postTransaction(jwtToken, request);
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
    Sep31TxnFields fields;

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

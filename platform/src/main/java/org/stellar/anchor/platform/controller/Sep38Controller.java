package org.stellar.anchor.platform.controller;

import static org.stellar.anchor.platform.controller.Sep10Helper.getSep10Token;
import static org.stellar.anchor.util.Log.*;

import javax.servlet.http.HttpServletRequest;
import lombok.SneakyThrows;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientException;
import org.stellar.anchor.dto.SepExceptionResponse;
import org.stellar.anchor.dto.sep38.*;
import org.stellar.anchor.sep10.JwtToken;
import org.stellar.anchor.sep38.Sep38Service;

@RestController
@RequestMapping("/sep38")
public class Sep38Controller {
  private final Sep38Service sep38Service;

  public Sep38Controller(Sep38Service sep38Service) {
    this.sep38Service = sep38Service;
  }
  // TODO: add integration tests

  @CrossOrigin(origins = "*")
  @RequestMapping(
      value = "/info",
      method = {RequestMethod.GET})
  public InfoResponse getInfo() {
    return sep38Service.getInfo();
  }

  @SneakyThrows
  @CrossOrigin(origins = "*")
  @RequestMapping(
      value = "/prices",
      method = {RequestMethod.GET})
  public GetPricesResponse getPrices(
      @RequestParam() String sellAssetName,
      @RequestParam() String sellAmount,
      @RequestParam(required = false) String sellDeliveryMethod,
      @RequestParam(required = false) String buyDeliveryMethod,
      @RequestParam(required = false) String countryCode) {
    return sep38Service.getPrices(
        sellAssetName, sellAmount, sellDeliveryMethod, buyDeliveryMethod, countryCode);
  }

  @SneakyThrows
  @CrossOrigin(origins = "*")
  @RequestMapping(
      value = "/price",
      method = {RequestMethod.GET})
  public GetPriceResponse getPrice(
      @RequestParam() String sellAssetName,
      @RequestParam(required = false) String sellAmount,
      @RequestParam() String sellDeliveryMethod,
      @RequestParam(required = false) String buyAssetName,
      @RequestParam(required = false) String buyAmount,
      @RequestParam(required = false) String buyDeliveryMethod,
      @RequestParam(required = false) String countryCode) {
    return sep38Service.getPrice(
        sellAssetName,
        sellAmount,
        sellDeliveryMethod,
        buyAssetName,
        buyAmount,
        buyDeliveryMethod,
        countryCode);
  }

  @SneakyThrows
  @CrossOrigin(origins = "*")
  @ResponseStatus(code = HttpStatus.CREATED)
  @RequestMapping(
      value = "/quote",
      consumes = {MediaType.APPLICATION_JSON_VALUE},
      method = {RequestMethod.POST})
  public Sep38QuoteResponse postQuote(
      HttpServletRequest request, @RequestBody Sep38PostQuoteRequest postQuoteRequest) {
    JwtToken jwtToken = getSep10Token(request);
    return sep38Service.postQuote(jwtToken, postQuoteRequest);
  }

  @ExceptionHandler(RestClientException.class)
  @ResponseStatus(value = HttpStatus.SERVICE_UNAVAILABLE)
  public SepExceptionResponse handleRestClientException(RestClientException ex) {
    errorEx(ex);
    return new SepExceptionResponse(ex.getMessage());
  }
}

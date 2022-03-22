package org.stellar.anchor.platform.controller;

import static org.stellar.anchor.util.Log.*;

import lombok.SneakyThrows;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientException;
import org.stellar.anchor.dto.SepExceptionResponse;
import org.stellar.anchor.dto.sep38.GetPriceResponse;
import org.stellar.anchor.dto.sep38.GetPricesResponse;
import org.stellar.anchor.dto.sep38.InfoResponse;
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

  @ExceptionHandler(RestClientException.class)
  @ResponseStatus(value = HttpStatus.SERVICE_UNAVAILABLE)
  public SepExceptionResponse handleRestClientException(RestClientException ex) {
    errorEx(ex);
    return new SepExceptionResponse(ex.getMessage());
  }
}

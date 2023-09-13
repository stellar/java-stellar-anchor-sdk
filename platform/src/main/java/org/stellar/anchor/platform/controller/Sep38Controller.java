package org.stellar.anchor.platform.controller;

import static org.stellar.anchor.platform.controller.Sep10Helper.getSep10Token;
import static org.stellar.anchor.util.Log.debugF;
import static org.stellar.anchor.util.Log.errorEx;

import com.google.gson.Gson;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import lombok.SneakyThrows;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientException;
import org.stellar.anchor.api.sep.SepExceptionResponse;
import org.stellar.anchor.api.sep.sep38.*;
import org.stellar.anchor.auth.JwtToken;
import org.stellar.anchor.platform.condition.ConditionalOnAllSepsEnabled;
import org.stellar.anchor.sep38.Sep38Service;
import org.stellar.anchor.util.GsonUtils;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/sep38")
@ConditionalOnAllSepsEnabled(seps = {"sep38"})
@Profile("default")
public class Sep38Controller {
  private final Sep38Service sep38Service;
  private static final Gson gson = GsonUtils.builder().create();

  public Sep38Controller(Sep38Service sep38Service) {
    this.sep38Service = sep38Service;
  }
  // TODO: add integration tests

  @CrossOrigin(origins = "*")
  @RequestMapping(
      value = "/info",
      method = {RequestMethod.GET})
  public InfoResponse getInfo() {
    debugF("GET /info");
    return sep38Service.getInfo();
  }

  @SneakyThrows
  @CrossOrigin(origins = "*")
  @RequestMapping(
      value = "/prices",
      method = {RequestMethod.GET})
  public GetPricesResponse getPrices(
      @RequestParam(name = "sell_asset") String sellAssetName,
      @RequestParam(name = "sell_amount") String sellAmount,
      @RequestParam(name = "sell_delivery_method", required = false) String sellDeliveryMethod,
      @RequestParam(name = "buy_delivery_method", required = false) String buyDeliveryMethod,
      @RequestParam(name = "country_code", required = false) String countryCode) {
    debugF(
        "GET /prices sell_asset={} sell_amount={} sell_delivery_method={} "
            + "buyDeliveryMethod={} countryCode={}",
        sellAssetName,
        sellAmount,
        sellDeliveryMethod,
        buyDeliveryMethod,
        countryCode);
    return sep38Service.getPrices(
        sellAssetName, sellAmount, sellDeliveryMethod, buyDeliveryMethod, countryCode);
  }

  @SneakyThrows
  @CrossOrigin(origins = "*")
  @RequestMapping(
      value = "/price",
      method = {RequestMethod.GET})
  public GetPriceResponse getPrice(@RequestParam Map<String, String> params) {
    debugF("GET /price params={}", params);
    Sep38GetPriceRequest getPriceRequest =
        gson.fromJson(gson.toJson(params), Sep38GetPriceRequest.class);
    return sep38Service.getPrice(getPriceRequest);
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
    debugF("POSTS /quote request={}", postQuoteRequest);
    return sep38Service.postQuote(jwtToken, postQuoteRequest);
  }

  @SneakyThrows
  @CrossOrigin(origins = "*")
  @ResponseStatus(code = HttpStatus.OK)
  @RequestMapping(
      value = "/quote/{quote_id}",
      method = {RequestMethod.GET})
  public Sep38QuoteResponse getQuote(
      HttpServletRequest request, @PathVariable(name = "quote_id") String quoteId) {
    JwtToken jwtToken = getSep10Token(request);
    debugF("GET /quote id={}", quoteId);
    return sep38Service.getQuote(jwtToken, quoteId);
  }

  @ExceptionHandler(RestClientException.class)
  @ResponseStatus(value = HttpStatus.SERVICE_UNAVAILABLE)
  public SepExceptionResponse handleRestClientException(RestClientException ex) {
    errorEx(ex);
    return new SepExceptionResponse(ex.getMessage());
  }
}

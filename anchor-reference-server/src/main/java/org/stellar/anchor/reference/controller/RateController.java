package org.stellar.anchor.reference.controller;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.web.bind.annotation.*;
import org.stellar.anchor.exception.AnchorException;
import org.stellar.anchor.reference.config.AppSettings;
import org.stellar.anchor.reference.service.RateService;
import org.stellar.platform.apis.callbacks.requests.GetRateRequest;
import org.stellar.platform.apis.callbacks.responses.GetRateResponse;

@RestController
public class RateController {
  private final RateService rateService;

  public RateController(AppSettings appSettings, RateService rateService) {
    this.rateService = rateService;
  }

  /** Gets a rate */
  @RequestMapping(
      value = "/rate",
      method = {RequestMethod.GET})
  @ResponseBody
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public GetRateResponse getRate(
      @RequestParam() String type,
      @RequestParam(required = false) String id,
      @RequestParam(name = "sell_asset") String sellAsset,
      @RequestParam(name = "sell_amount", required = false) String sellAmount,
      @RequestParam(name = "sell_delivery_method", required = false) String sellDeliveryMethod,
      @RequestParam(name = "buy_asset") String buyAsset,
      @RequestParam(name = "buy_amount", required = false) String buyAmount,
      @RequestParam(name = "buy_delivery_method", required = false) String buyDeliveryMethod,
      @RequestParam(name = "country_code", required = false) String countryCode,
      @RequestParam(name = "client_domain", required = false) String clientDomain,
      @RequestParam(required = false) String account,
      @RequestParam(required = false) String memo,
      @RequestParam(name = "memo_type", required = false) String memoType)
      throws AnchorException {
    return rateService.getRate(
        GetRateRequest.builder()
            .type(type)
            .id(id)
            .sellAsset(sellAsset)
            .sellAmount(sellAmount)
            .sellDeliveryMethod(sellDeliveryMethod)
            .buyAsset(buyAsset)
            .buyAmount(buyAmount)
            .buyDeliveryMethod(buyDeliveryMethod)
            .countryCode(countryCode)
            .clientDomain(clientDomain)
            .account(account)
            .memo(memo)
            .memoType(memoType)
            .build());
  }
}

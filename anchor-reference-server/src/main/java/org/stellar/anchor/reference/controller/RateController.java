package org.stellar.anchor.reference.controller;

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
  public GetRateResponse getRate(GetRateRequest getRateRequest) throws AnchorException {
    return rateService.getRate(getRateRequest);
  }
}

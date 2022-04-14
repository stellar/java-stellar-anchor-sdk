package org.stellar.anchor.reference.controller;

import com.google.gson.Gson;
import java.util.Map;
import org.springframework.web.bind.annotation.*;
import org.stellar.anchor.exception.AnchorException;
import org.stellar.anchor.reference.config.AppSettings;
import org.stellar.anchor.reference.service.RateService;
import org.stellar.anchor.util.GsonUtils;
import org.stellar.platform.apis.callbacks.requests.GetRateRequest;
import org.stellar.platform.apis.callbacks.responses.GetRateResponse;

@RestController
public class RateController {
  private final RateService rateService;
  private static final Gson gson = GsonUtils.builder().create();

  public RateController(AppSettings appSettings, RateService rateService) {
    this.rateService = rateService;
  }

  /** Gets a rate */
  @RequestMapping(
      value = "/rate",
      method = {RequestMethod.GET})
  @ResponseBody
  public GetRateResponse getRate(@RequestParam Map<String, String> params) throws AnchorException {
    GetRateRequest getRateRequest = gson.fromJson(gson.toJson(params), GetRateRequest.class);
    return rateService.getRate(getRateRequest);
  }
}

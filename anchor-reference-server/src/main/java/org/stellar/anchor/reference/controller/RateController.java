package org.stellar.anchor.reference.controller;

import com.google.gson.Gson;
import java.util.Map;
import org.springframework.web.bind.annotation.*;
import org.stellar.anchor.api.callback.GetRateRequest;
import org.stellar.anchor.api.callback.GetRateResponse;
import org.stellar.anchor.api.exception.AnchorException;
import org.stellar.anchor.reference.service.RateService;
import org.stellar.anchor.util.GsonUtils;

@RestController
public class RateController {
  private final RateService rateService;
  private static final Gson gson = GsonUtils.builder().create();

  public RateController(RateService rateService) {
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

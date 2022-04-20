package org.stellar.anchor.reference.controller;

import com.google.gson.Gson;
import lombok.SneakyThrows;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.stellar.anchor.reference.service.FeeService;
import org.stellar.anchor.util.GsonUtils;
import org.stellar.platform.apis.callbacks.requests.GetFeeRequest;
import org.stellar.platform.apis.callbacks.responses.GetFeeResponse;

import java.util.Map;

@RestController
public class FeeController {
  private FeeService feeService;
  private static final Gson gson = GsonUtils.builder().create();

  public FeeController(FeeService feeService) {
    this.feeService = feeService;
  }

  @SneakyThrows
  @RequestMapping(
      value = "/fee",
      method = {RequestMethod.GET})
  public GetFeeResponse getFee(@RequestParam Map<String, String> params) {
    GetFeeRequest request = gson.fromJson(gson.toJson(params), GetFeeRequest.class);
    return feeService.getFee(request);
  }
}

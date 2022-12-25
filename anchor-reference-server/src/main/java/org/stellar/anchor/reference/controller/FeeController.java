package org.stellar.anchor.reference.controller;

import com.google.gson.Gson;
import java.util.Map;
import lombok.SneakyThrows;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.stellar.anchor.api.callback.GetFeeRequest;
import org.stellar.anchor.api.callback.GetFeeResponse;
import org.stellar.anchor.reference.service.FeeService;
import org.stellar.anchor.util.GsonUtils;

@RestController
public class FeeController {
  final FeeService feeService;
  static final Gson gson = GsonUtils.getInstance();

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

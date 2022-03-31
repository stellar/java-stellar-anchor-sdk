package org.stellar.anchor.reference.controller;

import com.google.gson.Gson;
import lombok.SneakyThrows;
import org.springframework.web.bind.annotation.*;
import org.stellar.anchor.exception.NotFoundException;
import org.stellar.anchor.reference.config.AppSettings;
import org.stellar.anchor.reference.service.CustomerService;
import org.stellar.anchor.reference.service.FeeService;
import org.stellar.anchor.util.GsonUtils;
import org.stellar.platform.apis.callbacks.requests.*;
import org.stellar.platform.apis.callbacks.responses.DeleteCustomerResponse;
import org.stellar.platform.apis.callbacks.responses.GetCustomerResponse;
import org.stellar.platform.apis.callbacks.responses.GetFeeResponse;
import org.stellar.platform.apis.callbacks.responses.PutCustomerResponse;

import java.util.Map;

@RestController
public class FeeController {
  private FeeService feeService;
  private static final Gson gson = GsonUtils.builder().create();

  public FeeController(AppSettings appSettings, FeeService feeService) {
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

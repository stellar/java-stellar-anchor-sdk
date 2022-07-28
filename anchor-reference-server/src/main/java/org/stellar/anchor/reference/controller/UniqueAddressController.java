package org.stellar.anchor.reference.controller;

import com.google.gson.Gson;
import org.springframework.web.bind.annotation.*;
import org.stellar.anchor.api.callback.GetUniqueAddressResponse;
import org.stellar.anchor.reference.service.UniqueAddressService;
import org.stellar.anchor.util.GsonUtils;

@RestController
public class UniqueAddressController {
  final UniqueAddressService uniqueAddressService;
  static final Gson gson = GsonUtils.builder().create();

  public UniqueAddressController(UniqueAddressService uniqueAddressService) {
    this.uniqueAddressService = uniqueAddressService;
  }

  @RequestMapping(
      value = "/unique_address",
      method = {RequestMethod.GET})
  @ResponseBody
  public GetUniqueAddressResponse getRate(
      @RequestParam(name = "transaction_id") String transactionId) {
    return uniqueAddressService.getUniqueAddress(transactionId);
  }
}

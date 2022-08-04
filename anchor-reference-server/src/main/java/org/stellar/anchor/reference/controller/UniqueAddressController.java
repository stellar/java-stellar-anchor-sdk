package org.stellar.anchor.reference.controller;

import org.springframework.web.bind.annotation.*;
import org.stellar.anchor.api.callback.GetUniqueAddressResponse;
import org.stellar.anchor.reference.service.UniqueAddressService;
import org.stellar.anchor.util.ConditionalOnPropertyNotEmpty;

@RestController
@ConditionalOnPropertyNotEmpty("anchor.settings.distributionWallet")
public class UniqueAddressController {
  final UniqueAddressService uniqueAddressService;

  public UniqueAddressController(UniqueAddressService uniqueAddressService) {
    this.uniqueAddressService = uniqueAddressService;
  }

  @RequestMapping(
      value = "/unique_address",
      method = {RequestMethod.GET})
  @ResponseBody
  public GetUniqueAddressResponse getUniqueAddress(
      @RequestParam(name = "transaction_id") String transactionId) {
    return uniqueAddressService.getUniqueAddress(transactionId);
  }
}

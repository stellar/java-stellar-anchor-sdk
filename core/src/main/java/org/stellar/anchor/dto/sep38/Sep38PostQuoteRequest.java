package org.stellar.anchor.dto.sep38;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Sep38PostQuoteRequest {
  String sellAssetName;
  String sellAmount;
  String sellDeliveryMethod;
  String buyAssetName;
  String buyAmount;
  String buyDeliveryMethod;
  String countryCode;
  String expireAfter;
}

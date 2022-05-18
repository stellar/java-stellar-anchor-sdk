package org.stellar.anchor.sep38;

import java.time.Instant;
import java.util.List;
import lombok.Data;
import org.stellar.anchor.api.sep.sep38.PriceDetail;

@Data
public class PojoSep38Quote implements Sep38Quote {
  String id;
  Instant expiresAt;
  String price;
  String sellAsset;
  String sellAmount;
  String sellDeliveryMethod;
  String buyAsset;
  String buyAmount;
  String buyDeliveryMethod;
  Instant createdAt;
  String creatorAccountId;
  String creatorMemo;
  String creatorMemoType;
  String transactionId;
  List<PriceDetail> priceDetails;
}

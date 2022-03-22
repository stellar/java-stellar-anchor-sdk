package org.stellar.anchor.sep38;

import java.time.LocalDateTime;
import lombok.Data;
import org.stellar.anchor.model.Sep38Quote;

@Data
public class PojoSep38Quote implements Sep38Quote {
  String id;
  LocalDateTime expiresAt;
  String price;
  String sellAsset;
  String sellAmount;
  String sellDeliveryMethod;
  String buyAsset;
  String buyAmount;
  String buyDeliveryMethod;
  LocalDateTime createdAt;
  String creatorAccountId;
  String creatorMemo;
  String transactionId;
}

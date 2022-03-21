package org.stellar.anchor.reference.model;

import java.time.LocalDateTime;
import javax.persistence.Entity;
import javax.persistence.Id;
import lombok.Data;

@Data
@Entity
public class Quote {
  @Id String id;

  String price;

  LocalDateTime expiresAt;

  LocalDateTime createdAt;

  String sellAsset;

  String sellAmount;

  String sellDeliveryMethod;

  String buyAsset;

  String buyAmount;

  String buyDeliveryMethod;

  String countryCode;

  String clientDomain;

  String stellarAccount;

  String memo;

  String memoType;

  String transactionId;
}

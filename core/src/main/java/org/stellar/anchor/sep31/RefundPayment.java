package org.stellar.anchor.sep31;

import org.stellar.anchor.api.shared.Amount;

public interface RefundPayment {
  String getId();

  void setId(String id);

  String getAmount();

  void setAmount(String amount);

  String getFee();

  void setFee(String fee);

  default org.stellar.anchor.api.shared.RefundPayment toPlatformApiRefundPayment(String assetName) {
    return org.stellar.anchor.api.shared.RefundPayment.builder()
        .id(getId())
        .idType(org.stellar.anchor.api.shared.RefundPayment.IdType.STELLAR)
        .amount(new Amount(getAmount(), assetName))
        .fee(new Amount(getFee(), assetName))
        .requestedAt(null)
        .refundedAt(null)
        .build();
  }
}

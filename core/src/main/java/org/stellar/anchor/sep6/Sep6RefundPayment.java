package org.stellar.anchor.sep6;

public interface Sep6RefundPayment {
  String getId();

  void setId(String id);

  String getAmount();

  void setAmount(String amount);

  String getFee();

  void setFee(String fee);

  static Sep6RefundPayment of(
      org.stellar.anchor.api.shared.RefundPayment platformApiRefundPayment,
      Sep6TransactionStore factory) {
    if (platformApiRefundPayment == null) return null;

    Sep6RefundPayment refundPayment = factory.newRefundPayment();
    refundPayment.setId(platformApiRefundPayment.getId());
    refundPayment.setAmount(platformApiRefundPayment.getAmount().getAmount());
    refundPayment.setFee(platformApiRefundPayment.getFee().getAmount());

    return refundPayment;
  }
}

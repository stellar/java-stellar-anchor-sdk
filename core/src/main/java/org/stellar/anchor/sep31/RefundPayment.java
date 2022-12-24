package org.stellar.anchor.sep31;

import org.stellar.anchor.api.sep.sep31.Sep31GetTransactionResponse;

public interface RefundPayment {
  String getId();

  void setId(String id);

  String getAmount();

  void setAmount(String amount);

  String getFee();

  void setFee(String fee);

  /**
   * Will create a Sep31GetTransactionResponse.Sep31RefundPayment object out of this SEP-31
   * RefundPayment object.
   *
   * @return a Sep31GetTransactionResponse.Sep31RefundPayment.
   */
  default Sep31GetTransactionResponse.Sep31RefundPayment toSep31RefundPayment() {
    return Sep31GetTransactionResponse.Sep31RefundPayment.builder()
        .id(getId())
        .amount(getAmount())
        .fee(getFee())
        .build();
  }

  /**
   * Will create a SEP-31 RefundPayment object out of a PlatformApi RefundPayment object.
   *
   * @param platformApiRefundPayment is the platformApi's RefundPayment object.
   * @param factory is a Sep31TransactionStore instance used to build the object.
   * @return a SEP-31 RefundPayment object.
   */
  static RefundPayment of(
      org.stellar.anchor.api.shared.RefundPayment platformApiRefundPayment,
      Sep31TransactionStore factory) {
    if (platformApiRefundPayment == null) {
      return null;
    }

    RefundPayment refundPayment = factory.newRefundPayment();
    refundPayment.setId(platformApiRefundPayment.getId());
    refundPayment.setAmount(platformApiRefundPayment.getAmount().getAmount());
    refundPayment.setFee(platformApiRefundPayment.getFee().getAmount());
    return refundPayment;
  }
}

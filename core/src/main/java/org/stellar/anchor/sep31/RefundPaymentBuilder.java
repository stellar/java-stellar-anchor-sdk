package org.stellar.anchor.sep31;

public class RefundPaymentBuilder {
  private final RefundPayment refundPayment;

  public RefundPaymentBuilder(Sep31TransactionStore factory) {
    refundPayment = factory.newRefundPayment();
  }

  public RefundPaymentBuilder id(String id) {
    refundPayment.setId(id);
    return this;
  }

  public RefundPaymentBuilder amount(String amount) {
    refundPayment.setAmount(amount);
    return this;
  }

  public RefundPaymentBuilder fee(String fee) {
    refundPayment.setFee(fee);
    return this;
  }

  public RefundPayment build() {
    return refundPayment;
  }

  /**
   * loadPlatformApiRefundPayment will load the values from the PlatformApi RefundPayment object
   * into the SEP-31 RefundPayment object.
   *
   * @param platformApiRefundPayment is the platformApi's RefundPayment object.
   * @return a SEP-31 RefundPayment object.
   */
  public RefundPayment loadPlatformApiRefundPayment(
      org.stellar.anchor.api.shared.RefundPayment platformApiRefundPayment) {
    if (platformApiRefundPayment == null) {
      return null;
    }

    return this.id(platformApiRefundPayment.getId())
        .amount(platformApiRefundPayment.getAmount().getAmount())
        .fee(platformApiRefundPayment.getFee().getAmount())
        .build();
  }
}

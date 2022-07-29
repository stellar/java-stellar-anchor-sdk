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
}

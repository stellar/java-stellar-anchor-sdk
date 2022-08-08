package org.stellar.anchor.sep31;

import java.util.ArrayList;
import java.util.List;

public class RefundsBuilder {
  private final Refunds refunds;
  private final Sep31TransactionStore factory;

  public RefundsBuilder(Sep31TransactionStore factory) {
    this.factory = factory;
    refunds = factory.newRefunds();
  }

  public RefundsBuilder amountRefunded(String amountRefunded) {
    refunds.setAmountRefunded(amountRefunded);
    return this;
  }

  public RefundsBuilder amountFee(String amountFee) {
    refunds.setAmountFee(amountFee);
    return this;
  }

  public RefundsBuilder payments(List<RefundPayment> payments) {
    refunds.setRefundPayments(payments);
    return this;
  }

  public Refunds build() {
    return refunds;
  }

  /**
   * loadPlatformApiRefunds will load the values from the PlatformApi Refund object into the SEP-31
   * Refunds object.
   *
   * @param platformApiRefunds is the platformApi's Refund object.
   * @return a SEP-31 Refunds object.
   */
  public Refunds loadPlatformApiRefunds(org.stellar.anchor.api.shared.Refund platformApiRefunds) {
    if (platformApiRefunds == null) {
      return null;
    }

    ArrayList<RefundPayment> payments = null;
    for (org.stellar.anchor.api.shared.RefundPayment platformApiRefundPayment :
        platformApiRefunds.getPayments()) {
      if (payments == null) {
        payments = new ArrayList<>();
      }

      payments.add(
          new RefundPaymentBuilder(this.factory)
              .loadPlatformApiRefundPayment(platformApiRefundPayment));
    }

    return this.amountRefunded(platformApiRefunds.getAmountRefunded().getAmount())
        .amountFee(platformApiRefunds.getAmountFee().getAmount())
        .payments(payments)
        .build();
  }
}

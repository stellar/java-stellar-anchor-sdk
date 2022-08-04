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

  public Refunds fromPlatformApiRefunds(org.stellar.anchor.api.shared.Refund platformApiRefunds) {
    ArrayList<RefundPayment> payments = null;
    for (org.stellar.anchor.api.shared.RefundPayment platformApiRefundPayment :
        platformApiRefunds.getPayments()) {
      if (payments == null) {
        payments = new ArrayList<>();
      }

      RefundPayment newRefundPayment =
          new RefundPaymentBuilder(this.factory)
              .id(platformApiRefundPayment.getId())
              .amount(platformApiRefundPayment.getAmount().getAmount())
              .fee(platformApiRefundPayment.getFee().getAmount())
              .build();
      payments.add(newRefundPayment);
    }

    return this.amountRefunded(platformApiRefunds.getAmountRefunded().getAmount())
        .amountFee(platformApiRefunds.getAmountFee().getAmount())
        .payments(payments)
        .build();
  }
}

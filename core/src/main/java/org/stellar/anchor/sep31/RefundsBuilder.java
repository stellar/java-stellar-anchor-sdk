package org.stellar.anchor.sep31;

import java.util.List;

public class RefundsBuilder {
  private final Sep31Refunds sep31Refunds;

  public RefundsBuilder(Sep31TransactionStore factory) {
    sep31Refunds = factory.newRefunds();
  }

  public RefundsBuilder amountRefunded(String amountRefunded) {
    sep31Refunds.setAmountRefunded(amountRefunded);
    return this;
  }

  public RefundsBuilder amountFee(String amountFee) {
    sep31Refunds.setAmountFee(amountFee);
    return this;
  }

  public RefundsBuilder payments(List<RefundPayment> payments) {
    sep31Refunds.setRefundPayments(payments);
    return this;
  }

  public Sep31Refunds build() {
    return sep31Refunds;
  }
}

package org.stellar.anchor.sep31;

import java.util.List;

public class RefundsBuilder {
  private final Sep31Transaction.Refunds refunds;

  public RefundsBuilder(Sep31TransactionStore factory) {
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

  public RefundsBuilder payments(List<Sep31Transaction.RefundPayment> payments) {
    refunds.setRefundPayments(payments);
    return this;
  }

  public Sep31Transaction.Refunds build() {
    return refunds;
  }
}

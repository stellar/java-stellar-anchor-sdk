package org.stellar.anchor.sep31;

import java.util.List;

public class RefundsBuilder {
  Sep31Transaction.Refunds refunds;

  RefundsBuilder(Sep31TransactionStore factory) {
    refunds = factory.newRefunds();
  }

  RefundsBuilder amountRefunded(String amountRefunded) {
    refunds.setAmountRefunded(amountRefunded);
    return this;
  }

  RefundsBuilder amountFee(String amountFee) {
    refunds.setAmountFee(amountFee);
    return this;
  }

  RefundsBuilder payments(List<Sep31Transaction.RefundPayment> payments) {
    refunds.setRefundPayments(payments);
    return this;
  }

  Sep31Transaction.Refunds build() {
    return refunds;
  }
}

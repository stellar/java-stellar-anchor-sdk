package org.stellar.anchor.sep24;

import java.util.List;

public interface Sep24Refunds {
  String getAmountRefunded();

  String getAmountFee();

  List<? extends Sep24RefundPayment> getPayments();
}

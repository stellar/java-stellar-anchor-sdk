package org.stellar.anchor.sep24;

import java.util.List;

@SuppressWarnings("unused")
public interface Sep24Refunds {
  String getAmountRefunded();

  void setAmountRefunded(String amountRefunded);

  String getAmountFee();

  void setAmountFee(String amountFee);

  List<Sep24RefundPayment> getRefundPayments();

  void setRefundPayments(List<Sep24RefundPayment> refundPayments);
}

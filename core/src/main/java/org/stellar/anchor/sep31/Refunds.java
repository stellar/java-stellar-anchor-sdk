package org.stellar.anchor.sep31;

import java.util.List;

public interface Refunds {

  String getAmountRefunded();

  void setAmountRefunded(String amountRefunded);

  String getAmountFee();

  void setAmountFee(String amountFee);

  List<RefundPayment> getRefundPayments();

  void setRefundPayments(List<RefundPayment> refundPayments);
}

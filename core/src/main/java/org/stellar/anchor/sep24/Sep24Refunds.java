package org.stellar.anchor.sep24;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.stellar.anchor.api.shared.Refunds;

@SuppressWarnings("unused")
public interface Sep24Refunds {
  String getAmountRefunded();

  void setAmountRefunded(String amountRefunded);

  String getAmountFee();

  void setAmountFee(String amountFee);

  List<Sep24RefundPayment> getRefundPayments();

  void setRefundPayments(List<Sep24RefundPayment> refundPayments);

  static Sep24Refunds of(Refunds platformApiRefunds, Sep24TransactionStore factory) {
    if (platformApiRefunds == null) {
      return null;
    }

    Sep24Refunds refunds = factory.newRefunds();
    refunds.setAmountRefunded(platformApiRefunds.getAmountRefunded().getAmount());
    refunds.setAmountFee(platformApiRefunds.getAmountFee().getAmount());

    ArrayList<Sep24RefundPayment> payments =
        Arrays.stream(platformApiRefunds.getPayments())
            .map(refundPayment -> Sep24RefundPayment.of(refundPayment, factory))
            .collect(Collectors.toCollection(ArrayList::new));
    refunds.setRefundPayments(payments);

    return refunds;
  }
}

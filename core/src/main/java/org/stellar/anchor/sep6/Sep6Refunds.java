package org.stellar.anchor.sep6;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.stellar.anchor.api.shared.Refunds;

public interface Sep6Refunds {
  String getAmountRefunded();

  void setAmountRefunded(String amountRefunded);

  String getAmountFee();

  void setAmountFee(String amountFee);

  List<Sep6RefundPayment> getPayments();

  void setPayments(List<Sep6RefundPayment> refundPayments);

  static Sep6Refunds of(Refunds platformApiRefunds, Sep6TransactionStore factory) {
    if (platformApiRefunds == null) {
      return null;
    }

    Sep6Refunds refunds = factory.newRefunds();
    refunds.setAmountRefunded(platformApiRefunds.getAmountRefunded().getAmount());
    refunds.setAmountFee(platformApiRefunds.getAmountFee().getAmount());

    ArrayList<Sep6RefundPayment> payments =
        Arrays.stream(platformApiRefunds.getPayments())
            .map(refundPayment -> Sep6RefundPayment.of(refundPayment, factory))
            .collect(Collectors.toCollection(ArrayList::new));
    refunds.setPayments(payments);

    return refunds;
  }
}

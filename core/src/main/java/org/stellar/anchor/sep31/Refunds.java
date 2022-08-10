package org.stellar.anchor.sep31;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.stellar.anchor.api.shared.Amount;

public interface Refunds {

  String getAmountRefunded();

  void setAmountRefunded(String amountRefunded);

  String getAmountFee();

  void setAmountFee(String amountFee);

  List<RefundPayment> getRefundPayments();

  void setRefundPayments(List<RefundPayment> refundPayments);

  default org.stellar.anchor.api.shared.Refund toPlatformApiRefund(String assetName) {
    // build payments
    org.stellar.anchor.api.shared.RefundPayment[] payments = null;
    for (int i = 0; i < getRefundPayments().size(); i++) {
      org.stellar.anchor.api.shared.RefundPayment platformRefundPayment =
          getRefundPayments().get(i).toPlatformApiRefundPayment(assetName);
      if (payments == null) {
        payments = new org.stellar.anchor.api.shared.RefundPayment[getRefundPayments().size()];
      }
      payments[i] = platformRefundPayment;
    }

    return org.stellar.anchor.api.shared.Refund.builder()
        .amountRefunded(new Amount(getAmountRefunded(), assetName))
        .amountFee(new Amount(getAmountFee(), assetName))
        .payments(payments)
        .build();
  }

  /**
   * Will create a SEP-31 Refunds object out of a PlatformApi Refund object.
   *
   * @param platformApiRefunds is the platformApi's Refund object.
   * @param factory is a Sep31TransactionStore instance used to build the object.
   * @return a SEP-31 Refunds.
   */
  static Refunds of(
      org.stellar.anchor.api.shared.Refund platformApiRefunds, Sep31TransactionStore factory) {
    if (platformApiRefunds == null) {
      return null;
    }

    Refunds refunds = factory.newRefunds();
    refunds.setAmountRefunded(platformApiRefunds.getAmountRefunded().getAmount());
    refunds.setAmountFee(platformApiRefunds.getAmountFee().getAmount());

    ArrayList<RefundPayment> payments =
        Arrays.stream(platformApiRefunds.getPayments())
            .map(refundPayment -> RefundPayment.of(refundPayment, factory))
            .collect(Collectors.toCollection(ArrayList::new));
    refunds.setRefundPayments(payments);

    return refunds;
  }
}

package org.stellar.anchor.sep31;

import java.util.List;
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
}

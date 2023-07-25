package org.stellar.anchor.sep6;

import java.util.List;

public interface Sep6Refunds {

  /**
   * The total amount refunded to the user, in units of <code>amount_in_asset</code>. If a full
   * refund was issued, this amount should match <code>amount_in</code>.
   *
   * @return the total amount refunded.
   */
  String getAmountRefunded();

  void setAmountRefunded(String amountRefunded);

  /**
   * The total amount charged in fees for processing all refund payments, in units of <code>
   * amount_in_asset</code>. The sum of all fees in the payments should equal this amount.
   *
   * @return the total fee amount.
   */
  String getAmountFee();

  void setAmountFee(String amountFee);

  /**
   * The list of refund payments that were issued to the user.
   *
   * @return the list of refund payments.
   */
  List<Sep6RefundPayment> getPayments();

  void setPayments(List<Sep6RefundPayment> refundPayments);
}

package org.stellar.anchor.sep31;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.stellar.anchor.api.sep.sep31.Sep31GetTransactionResponse;
import org.stellar.anchor.api.shared.Refunds;

public interface Sep31Refunds {

  String getAmountRefunded();

  void setAmountRefunded(String amountRefunded);

  String getAmountFee();

  void setAmountFee(String amountFee);

  List<RefundPayment> getRefundPayments();

  void setRefundPayments(List<RefundPayment> refundPayments);

  /**
   * Will create a Sep31GetTransactionResponse.Sep31Refunds object out of this SEP-31 Sep31Refunds
   * object.
   *
   * @return a Sep31GetTransactionResponse.Sep31Refunds object.
   */
  default Sep31GetTransactionResponse.Refunds toSep31TransactionResponseRefunds() {
    List<Sep31GetTransactionResponse.Sep31RefundPayment> payments =
        getRefundPayments().stream()
            .map(RefundPayment::toSep31RefundPayment)
            .collect(Collectors.toList());

    return Sep31GetTransactionResponse.Refunds.builder()
        .amountRefunded(getAmountRefunded())
        .amountFee(getAmountFee())
        .payments(payments)
        .build();
  }

  /**
   * Will create a SEP-31 Sep31Refunds object out of a PlatformApi Refunds object.
   *
   * @param platformApiRefunds is the platformApi's Refunds object.
   * @param factory is a Sep31TransactionStore instance used to build the object.
   * @return a SEP-31 Sep31Refunds object.
   */
  static Sep31Refunds of(Refunds platformApiRefunds, Sep31TransactionStore factory) {
    if (platformApiRefunds == null) {
      return null;
    }

    Sep31Refunds sep31Refunds = factory.newRefunds();
    sep31Refunds.setAmountRefunded(platformApiRefunds.getAmountRefunded().getAmount());
    sep31Refunds.setAmountFee(platformApiRefunds.getAmountFee().getAmount());

    ArrayList<RefundPayment> payments =
        Arrays.stream(platformApiRefunds.getPayments())
            .map(refundPayment -> RefundPayment.of(refundPayment, factory))
            .collect(Collectors.toCollection(ArrayList::new));
    sep31Refunds.setRefundPayments(payments);

    return sep31Refunds;
  }
}

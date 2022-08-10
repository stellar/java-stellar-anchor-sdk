package org.stellar.anchor.sep31;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.stellar.anchor.api.sep.sep31.Sep31GetTransactionResponse;
import org.stellar.anchor.api.shared.Amount;

public interface Refunds {

  String getAmountRefunded();

  void setAmountRefunded(String amountRefunded);

  String getAmountFee();

  void setAmountFee(String amountFee);

  List<RefundPayment> getRefundPayments();

  void setRefundPayments(List<RefundPayment> refundPayments);

  /**
   * Will create a Sep31GetTransactionResponse.Refunds object out of this SEP-31 Refunds object.
   *
   * @return a Sep31GetTransactionResponse.Refunds object.
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
   * Will create a PlatformApi Refund object out of this SEP-31 Refunds object.
   *
   * @param assetName is the full asset name in the {schema}:{code}:{issuer} format.
   * @return a PlatformApi Refund object.
   */
  default org.stellar.anchor.api.shared.Refund toPlatformApiRefund(String assetName) {
    // build payments
    org.stellar.anchor.api.shared.RefundPayment[] payments =
        getRefundPayments().stream()
            .map(refundPayment -> refundPayment.toPlatformApiRefundPayment(assetName))
            .toArray(org.stellar.anchor.api.shared.RefundPayment[]::new);

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
   * @return a SEP-31 Refunds object.
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

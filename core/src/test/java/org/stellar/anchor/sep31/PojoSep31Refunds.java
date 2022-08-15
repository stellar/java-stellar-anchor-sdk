package org.stellar.anchor.sep31;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class PojoSep31Refunds implements Refunds {
  String amountRefunded;
  String amountFee;
  List<PojoSep31RefundPayment> refundPayments;

  @Override
  public void setRefundPayments(List<RefundPayment> refundPayments) {
    if (refundPayments == null) {
      this.refundPayments = null;
      return;
    }

    List<PojoSep31RefundPayment> newRefundPayments = new ArrayList<>();
    for (RefundPayment rp : refundPayments) {
      newRefundPayments.add(new PojoSep31RefundPayment(rp.getId(), rp.getAmount(), rp.getFee()));
    }
    this.refundPayments = newRefundPayments;
  }

  @Override
  public List<RefundPayment> getRefundPayments() {
    if (this.refundPayments == null) {
      return null;
    }
    return new ArrayList<>(this.refundPayments);
  }
}

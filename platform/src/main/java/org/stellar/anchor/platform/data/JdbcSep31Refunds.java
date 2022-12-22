package org.stellar.anchor.platform.data;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import org.stellar.anchor.sep31.RefundPayment;
import org.stellar.anchor.sep31.Sep31Refunds;

@Data
public class JdbcSep31Refunds implements Sep31Refunds {
  @SerializedName("amount_refunded")
  String amountRefunded;

  @SerializedName("amount_fee")
  String amountFee;

  @SerializedName("payments")
  List<JdbcSep31RefundPayment> refundPayments;

  @Override
  public List<RefundPayment> getRefundPayments() {
    if (refundPayments == null) return null;
    // getPayments() is made for Gson serialization.
    List<RefundPayment> payments = new ArrayList<>(refundPayments.size());
    payments.addAll(refundPayments);
    return payments;
  }

  @Override
  public void setRefundPayments(List<RefundPayment> refundPayments) {
    this.refundPayments = new ArrayList<>(refundPayments.size());
    for (RefundPayment rp : refundPayments) {
      if (rp instanceof JdbcSep31RefundPayment)
        this.refundPayments.add((JdbcSep31RefundPayment) rp);
      else
        throw new ClassCastException(
            String.format("Error casting %s to JdbcSep31RefundPayment", rp.getClass()));
    }
  }
}

package org.stellar.anchor.platform.data;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.stellar.anchor.sep24.Sep24RefundPayment;
import org.stellar.anchor.sep24.Sep24Refunds;

@Getter
@Setter
@NoArgsConstructor
public class JdbcSep24Refunds implements Sep24Refunds {
  @SerializedName("amount_refunded")
  String amountRefunded;

  @SerializedName("amount_fee")
  String amountFee;

  @SerializedName("payments")
  List<JdbcSep24RefundPayment> refundPayments;

  @Override
  public List<Sep24RefundPayment> getRefundPayments() {
    if (refundPayments == null) return null;
    // getPayments() is made for Gson serialization.
    List<Sep24RefundPayment> payments = new ArrayList<>(refundPayments.size());
    payments.addAll(refundPayments);
    return payments;
  }

  @Override
  public void setRefundPayments(List<Sep24RefundPayment> refundPayments) {
    this.refundPayments = new ArrayList<>(refundPayments.size());
    for (Sep24RefundPayment rp : refundPayments) {
      if (rp instanceof JdbcSep24RefundPayment)
        this.refundPayments.add((JdbcSep24RefundPayment) rp);
      else
        throw new ClassCastException(
            String.format("Error casting %s to JdbcSep24RefundPayment", rp.getClass()));
    }
  }
}

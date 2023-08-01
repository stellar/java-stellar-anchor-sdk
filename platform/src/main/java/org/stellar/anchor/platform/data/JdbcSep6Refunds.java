package org.stellar.anchor.platform.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.stellar.anchor.sep6.Sep6RefundPayment;
import org.stellar.anchor.sep6.Sep6Refunds;

@Getter
@Setter
@NoArgsConstructor
public class JdbcSep6Refunds implements Sep6Refunds {
  @SerializedName("amount_refunded")
  String amountRefunded;

  @SerializedName("amount_fee")
  String amountFee;

  @SerializedName("payments")
  List<Sep6RefundPayment> payments;

  @JsonIgnore
  public List<Sep6RefundPayment> getRefundPayments() {
    if (payments == null) return null;
    List<Sep6RefundPayment> payments = new ArrayList<>(this.payments.size());
    payments.addAll(this.payments);
    return payments;
  }

  public void setRefundPayments(List<Sep6RefundPayment> refundPayments) {
    this.payments = new ArrayList<>(refundPayments.size());
    for (Sep6RefundPayment rp : refundPayments) {
      if (rp instanceof JdbcSep6RefundPayment) this.payments.add((JdbcSep6RefundPayment) rp);
      else
        throw new ClassCastException(
            String.format("Error casting %s to JdbcSep6RefundPayment", rp.getClass()));
    }
  }
}

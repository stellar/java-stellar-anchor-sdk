package org.stellar.anchor.platform.data;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import lombok.Data;
import org.stellar.anchor.sep31.RefundPayment;
import org.stellar.anchor.sep31.Refunds;

@Data
public class JdbcSep31Refunds implements Refunds {
  @SerializedName("amount_refunded")
  String amountRefunded;

  @SerializedName("amount_fee")
  String amountFee;

  @SerializedName("payments")
  List<RefundPayment> refundPayments;
}

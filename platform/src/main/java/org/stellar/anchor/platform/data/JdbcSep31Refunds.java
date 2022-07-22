package org.stellar.anchor.platform.data;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import lombok.Data;
import org.stellar.anchor.sep31.Sep31Transaction;

@Data
public class JdbcSep31Refunds implements Sep31Transaction.Refunds {
  @SerializedName("amount_refunded")
  String amountRefunded;

  @SerializedName("amount_fee")
  String amountFee;

  @SerializedName("payments")
  List<Sep31Transaction.RefundPayment> refundPayments;
}

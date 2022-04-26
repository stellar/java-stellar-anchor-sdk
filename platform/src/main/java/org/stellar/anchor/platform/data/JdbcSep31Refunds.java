package org.stellar.anchor.platform.data;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import lombok.Data;
import org.stellar.anchor.model.Sep31Transaction;

@Data
public class JdbcSep31Refunds implements Sep31Transaction.Refunds {
  @SerializedName("amount_refunded")
  String amountRefunded;

  @SerializedName("amount_fee")
  String amountFee;

  List<Sep31Transaction.RefundPayment> refundPayments;
}

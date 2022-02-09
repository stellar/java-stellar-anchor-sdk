package org.stellar.anchor.paymentservice.circle.model.response;

import java.util.List;
import lombok.Data;
import org.stellar.anchor.paymentservice.Account;
import org.stellar.anchor.paymentservice.PaymentHistory;
import org.stellar.anchor.paymentservice.circle.model.CirclePayout;

@Data
public class CirclePayoutListResponse {
  List<CirclePayout> data;

  public PaymentHistory toPaymentHistory(int pageSize, Account account) {
    PaymentHistory ph = new PaymentHistory(account);
    if (data == null || data.size() == 0) {
      return ph;
    }

    for (int i = 0; i < data.size(); i++) {
      CirclePayout payout = data.get(i);
      ph.getPayments().add(payout.toPayment());

      if (i == 0) {
        ph.setBeforeCursor(payout.getId());
      }

      if (i + 1 == pageSize) {
        ph.setAfterCursor(payout.getId());
      }
    }

    return ph;
  }
}

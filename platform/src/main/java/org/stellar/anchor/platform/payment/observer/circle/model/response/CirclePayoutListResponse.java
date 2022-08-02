package org.stellar.anchor.platform.payment.observer.circle.model.response;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.stellar.anchor.platform.payment.common.Account;
import org.stellar.anchor.platform.payment.common.PaymentHistory;
import org.stellar.anchor.platform.payment.observer.circle.model.CirclePayout;

@EqualsAndHashCode(callSuper = true)
@Data
public class CirclePayoutListResponse extends CircleListResponse<CirclePayout> {
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

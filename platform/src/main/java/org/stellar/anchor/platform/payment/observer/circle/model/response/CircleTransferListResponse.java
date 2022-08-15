package org.stellar.anchor.platform.payment.observer.circle.model.response;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.stellar.anchor.platform.payment.common.Account;
import org.stellar.anchor.platform.payment.common.PaymentHistory;
import org.stellar.anchor.platform.payment.observer.circle.model.CircleTransfer;

@EqualsAndHashCode(callSuper = true)
@Data
public class CircleTransferListResponse extends CircleListResponse<CircleTransfer> {
  public PaymentHistory toPaymentHistory(
      int pageSize, Account account, String distributionAccountId) {
    PaymentHistory ph = new PaymentHistory(account);
    if (data == null || data.size() == 0) {
      return ph;
    }

    for (int i = 0; i < data.size(); i++) {
      CircleTransfer transfer = data.get(i);
      ph.getPayments().add(transfer.toPayment(distributionAccountId));

      if (i == 0) {
        ph.setBeforeCursor(transfer.getId());
      }

      if (i + 1 == pageSize) {
        ph.setAfterCursor(transfer.getId());
      }
    }

    return ph;
  }
}

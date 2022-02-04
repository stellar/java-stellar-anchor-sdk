package org.stellar.anchor.paymentservice.circle.model.response;

import java.util.List;
import lombok.Data;
import org.stellar.anchor.paymentservice.Account;
import org.stellar.anchor.paymentservice.PaymentHistory;
import org.stellar.anchor.paymentservice.circle.model.CircleTransfer;

@Data
public class CircleTransferListResponse {
  List<CircleTransfer> data;

  public PaymentHistory toPaymentHistory(Integer pageSize, Account account) {
    PaymentHistory ph = new PaymentHistory(account);
    if (data == null || data.size() == 0) {
      return ph;
    }

    for (int i = 0; i < data.size(); i++) {
      CircleTransfer transfer = data.get(i);
      ph.getPayments().add(transfer.toPayment());

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

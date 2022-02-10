package org.stellar.anchor.paymentservice.circle;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import org.stellar.anchor.exception.HttpException;
import org.stellar.anchor.paymentservice.circle.model.CircleTransactionParty;
import org.stellar.anchor.paymentservice.circle.model.CircleTransfer;
import org.stellar.sdk.Server;
import org.stellar.sdk.responses.Page;
import org.stellar.sdk.responses.operations.OperationResponse;
import org.stellar.sdk.responses.operations.PathPaymentBaseOperationResponse;
import org.stellar.sdk.responses.operations.PaymentOperationResponse;

public interface StellarReconciliation {

  Server getHorizonServer();

  default void updateStellarSenderAddress(CircleTransfer transfer) throws HttpException {
    if (transfer.getSource().getId() != null) return;

    if (!transfer.getSource().getChain().equals("XLM")
        || transfer.getSource().getType() != CircleTransactionParty.Type.BLOCKCHAIN) {
      throw new HttpException(500, "invalid source account");
    }

    Page<OperationResponse> responsePage;
    try {
      responsePage =
          getHorizonServer().payments().forTransaction(transfer.getTransactionHash()).execute();
    } catch (IOException e) {
      e.printStackTrace();
      throw new HttpException(500, e.getMessage());
    }

    for (OperationResponse or : responsePage.getRecords()) {
      if (!or.isTransactionSuccessful()) continue;

      if (!List.of("payment", "path_payment_strict_send", "path_payment_strict_receive")
          .contains(or.getType())) continue;

      String amount, from;
      if ("payment".equals(or.getType())) {
        PaymentOperationResponse pr = (PaymentOperationResponse) or;
        amount = pr.getAmount();
        from = pr.getFrom();
      } else {
        PathPaymentBaseOperationResponse ppr = (PathPaymentBaseOperationResponse) or;
        amount = ppr.getAmount();
        from = ppr.getFrom();
      }

      BigDecimal wantAmount = new BigDecimal(transfer.getAmount().getAmount());
      BigDecimal gotAmount = new BigDecimal(amount);
      if (wantAmount.compareTo(gotAmount) != 0) continue;

      transfer.getSource().setAddress(from);
    }
  }
}

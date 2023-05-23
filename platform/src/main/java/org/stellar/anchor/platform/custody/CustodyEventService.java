package org.stellar.anchor.platform.custody;

import static org.stellar.anchor.api.platform.PlatformTransactionData.Kind.WITHDRAWAL;
import static org.stellar.anchor.util.Log.warnF;

import java.io.IOException;
import java.util.Map;
import org.stellar.anchor.api.exception.AnchorException;
import org.stellar.anchor.api.exception.BadRequestException;
import org.stellar.anchor.api.platform.PlatformTransactionData.Kind;
import org.stellar.anchor.platform.data.JdbcCustodyTransaction;
import org.stellar.anchor.platform.data.JdbcCustodyTransactionRepo;

/**
 * Basic class, that contains common logic to handle custody event. It links event to custody
 * transaction and pass event to appropriate handler. Event can be retrieved as webhook or as a
 * result of reconciliation job
 */
public abstract class CustodyEventService {

  private final JdbcCustodyTransactionRepo custodyTransactionRepo;
  private final Sep24CustodyPaymentHandler sep24CustodyPaymentHandler;
  private final Sep31CustodyPaymentHandler sep31CustodyPaymentHandler;

  protected CustodyEventService(
      JdbcCustodyTransactionRepo custodyTransactionRepo,
      Sep24CustodyPaymentHandler sep24CustodyPaymentHandler,
      Sep31CustodyPaymentHandler sep31CustodyPaymentHandler) {
    this.custodyTransactionRepo = custodyTransactionRepo;
    this.sep24CustodyPaymentHandler = sep24CustodyPaymentHandler;
    this.sep31CustodyPaymentHandler = sep31CustodyPaymentHandler;
  }

  public abstract void handleEvent(String event, Map<String, String> headers)
      throws BadRequestException;

  protected void handlePayment(CustodyPayment payment) throws AnchorException, IOException {
    JdbcCustodyTransaction custodyTransaction = getCustodyTransaction(payment);

    if (custodyTransaction == null) {
      warnF(
          "Custody transaction was not found. Payment: id[{}], externalTxId[{}]",
          payment.getId(),
          payment.getExternalTxId());
      return;
    }

    switch (custodyTransaction.getProtocol()) {
      case "24":
        if (Kind.DEPOSIT.getKind().equals(custodyTransaction.getKind())) {
          sep24CustodyPaymentHandler.onSent(custodyTransaction, payment);
          return;
        } else if (WITHDRAWAL.getKind().equals(custodyTransaction.getKind())) {
          sep24CustodyPaymentHandler.onReceived(custodyTransaction, payment);
          return;
        }
        break;
      case "31":
        if (Kind.RECEIVE.getKind().equals(custodyTransaction.getKind())) {
          sep31CustodyPaymentHandler.onReceived(custodyTransaction, payment);
          return;
        }
        break;
    }

    warnF(
        "Handler for custody transaction event with protocol[{}] and kind[{}] is not found. Payment: id[{}], externalTxId[{}]",
        custodyTransaction.getProtocol(),
        custodyTransaction.getKind(),
        payment.getId(),
        payment.getExternalTxId());
  }

  private JdbcCustodyTransaction getCustodyTransaction(CustodyPayment custodyPayment) {
    JdbcCustodyTransaction custodyTransaction =
        custodyTransactionRepo.findByExternalTxId(custodyPayment.getExternalTxId());

    if (custodyTransaction == null) {
      custodyTransaction =
          custodyTransactionRepo.findByToAccountAndMemo(
              custodyPayment.getTo(), custodyPayment.getTransactionMemo());
    }

    return custodyTransaction;
  }
}

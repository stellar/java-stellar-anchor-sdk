package org.stellar.anchor.platform.utils;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;
import static org.stellar.anchor.platform.service.PaymentOperationToEventListener.parsePaymentTime;
import static org.stellar.anchor.util.Log.error;

import java.util.List;
import java.util.Objects;
import org.stellar.anchor.api.exception.SepException;
import org.stellar.anchor.api.shared.Amount;
import org.stellar.anchor.api.shared.StellarPayment;
import org.stellar.anchor.api.shared.StellarTransaction;
import org.stellar.anchor.platform.data.JdbcSep24Transaction;
import org.stellar.anchor.platform.data.JdbcSepTransaction;
import org.stellar.anchor.platform.observer.ObservedPayment;
import org.stellar.sdk.responses.operations.OperationResponse;
import org.stellar.sdk.responses.operations.PathPaymentBaseOperationResponse;
import org.stellar.sdk.responses.operations.PaymentOperationResponse;

public class PaymentsUtil {

  public static void addStellarTransaction(
      JdbcSepTransaction txn, String stellarTransactionId, List<OperationResponse> operations) {

    List<ObservedPayment> payments = getObservedPayments(operations);

    if (!payments.isEmpty()) {
      ObservedPayment firstPayment = payments.get(0);
      ObservedPayment lastPayment = payments.get(payments.size() - 1);
      JdbcSep24Transaction txn24 = (JdbcSep24Transaction) txn;

      StellarTransaction stellarTransaction =
          StellarTransaction.builder()
              .id(stellarTransactionId)
              .createdAt(parsePaymentTime(firstPayment.getCreatedAt()))
              .memo(txn24.getMemo())
              .memoType(txn24.getMemoType())
              .envelope(firstPayment.getTransactionEnvelope())
              .payments(
                  payments.stream()
                      .map(
                          payment ->
                              StellarPayment.builder()
                                  .id(payment.getId())
                                  .amount(new Amount(payment.getAmount(), payment.getAssetName()))
                                  .sourceAccount(payment.getFrom())
                                  .destinationAccount(payment.getTo())
                                  .paymentType(
                                      payment.getType() == ObservedPayment.Type.PAYMENT
                                          ? StellarPayment.Type.PAYMENT
                                          : StellarPayment.Type.PATH_PAYMENT)
                                  .build())
                      .collect(toList()))
              .build();

      if (txn.getStellarTransactions() == null) {
        txn.setStellarTransactions(List.of(stellarTransaction));
      } else {
        txn.getStellarTransactions().add(stellarTransaction);
      }

      txn.setTransferReceivedAt(parsePaymentTime(lastPayment.getCreatedAt()));
      txn.setStellarTransactionId(stellarTransactionId);
    }
  }

  private static List<ObservedPayment> getObservedPayments(List<OperationResponse> payments) {
    return payments.stream()
        .map(
            operation -> {
              try {
                if (operation instanceof PaymentOperationResponse) {
                  return ObservedPayment.fromPaymentOperationResponse(
                      (PaymentOperationResponse) operation);
                } else if (operation instanceof PathPaymentBaseOperationResponse) {
                  return ObservedPayment.fromPathPaymentOperationResponse(
                      (PathPaymentBaseOperationResponse) operation);
                }
              } catch (SepException e) {
                error("Failed to parse operation response", e);
              }
              return null;
            })
        .filter(Objects::nonNull)
        .sorted(comparing(ObservedPayment::getCreatedAt))
        .collect(toList());
  }
}

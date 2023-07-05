package org.stellar.anchor.platform.action;

import static org.stellar.anchor.api.platform.PlatformTransactionData.Kind.WITHDRAWAL;
import static org.stellar.anchor.api.rpc.action.ActionMethod.NOTIFY_ONCHAIN_FUNDS_RECEIVED;
import static org.stellar.anchor.api.sep.SepTransactionStatus.PENDING_ANCHOR;
import static org.stellar.anchor.api.sep.SepTransactionStatus.PENDING_USR_TRANSFER_START;
import static org.stellar.anchor.platform.service.PaymentOperationToEventListener.parsePaymentTime;
import static org.stellar.anchor.util.Log.error;
import static org.stellar.anchor.util.Log.errorEx;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.validation.Validator;
import org.springframework.stereotype.Service;
import org.stellar.anchor.api.exception.AnchorException;
import org.stellar.anchor.api.exception.SepException;
import org.stellar.anchor.api.exception.rpc.InvalidParamsException;
import org.stellar.anchor.api.exception.rpc.InvalidRequestException;
import org.stellar.anchor.api.platform.PlatformTransactionData.Kind;
import org.stellar.anchor.api.rpc.action.ActionMethod;
import org.stellar.anchor.api.rpc.action.NotifyOnchainFundsReceivedRequest;
import org.stellar.anchor.api.sep.SepTransactionStatus;
import org.stellar.anchor.api.shared.Amount;
import org.stellar.anchor.api.shared.StellarPayment;
import org.stellar.anchor.api.shared.StellarTransaction;
import org.stellar.anchor.asset.AssetService;
import org.stellar.anchor.horizon.Horizon;
import org.stellar.anchor.platform.data.JdbcSep24Transaction;
import org.stellar.anchor.platform.data.JdbcSepTransaction;
import org.stellar.anchor.platform.observer.ObservedPayment;
import org.stellar.anchor.sep24.Sep24TransactionStore;
import org.stellar.anchor.sep31.Sep31TransactionStore;
import org.stellar.sdk.responses.operations.PathPaymentBaseOperationResponse;
import org.stellar.sdk.responses.operations.PaymentOperationResponse;

@Service
public class NotifyOnchainFundsReceivedHandler
    extends ActionHandler<NotifyOnchainFundsReceivedRequest> {

  public NotifyOnchainFundsReceivedHandler(
      Sep24TransactionStore txn24Store,
      Sep31TransactionStore txn31Store,
      Validator validator,
      Horizon horizon,
      AssetService assetService) {
    super(txn24Store, txn31Store, validator, horizon, assetService);
  }

  @Override
  public ActionMethod getActionType() {
    return NOTIFY_ONCHAIN_FUNDS_RECEIVED;
  }

  @Override
  protected SepTransactionStatus getNextStatus(
      JdbcSepTransaction txn, NotifyOnchainFundsReceivedRequest request)
      throws InvalidRequestException {
    JdbcSep24Transaction txn24 = (JdbcSep24Transaction) txn;
    if (WITHDRAWAL == Kind.from(txn24.getKind())) {
      return PENDING_ANCHOR;
    }
    throw new InvalidRequestException(
        String.format(
            "Invalid kind[%s] for protocol[%s] and action[%s]",
            txn24.getKind(), txn24.getProtocol(), getActionType()));
  }

  @Override
  protected Set<SepTransactionStatus> getSupportedStatuses(JdbcSepTransaction txn) {
    JdbcSep24Transaction txn24 = (JdbcSep24Transaction) txn;
    if (WITHDRAWAL == Kind.from(txn24.getKind())) {
      return Set.of(PENDING_USR_TRANSFER_START);
    }
    return Set.of();
  }

  @Override
  protected Set<String> getSupportedProtocols() {
    return Set.of("24");
  }

  @Override
  protected void updateTransactionWithAction(
      JdbcSepTransaction txn, NotifyOnchainFundsReceivedRequest request) throws AnchorException {

    if (request.getStellarTransactionId() == null) {
      throw new InvalidParamsException("stellar_transaction_id is required");
    }

    if (!((request.getAmountIn() == null
            && request.getAmountOut() == null
            && request.getAmountFee() == null)
        || (request.getAmountIn() != null
            && request.getAmountOut() != null
            && request.getAmountFee() != null)
        || (request.getAmountIn() != null
            && request.getAmountOut() == null
            && request.getAmountFee() == null))) {
      throw new InvalidParamsException(
          "Invalid amounts combination provided: all, none or only amount_in should be set");
    }

    try {
      List<ObservedPayment> payments = getObservedPayments(request.getStellarTransactionId());

      if (!payments.isEmpty()) {
        ObservedPayment firstPayment = payments.get(0);
        ObservedPayment lastPayment = payments.get(payments.size() - 1);
        JdbcSep24Transaction txn24 = (JdbcSep24Transaction) txn;

        StellarTransaction stellarTransaction =
            StellarTransaction.builder()
                .id(request.getStellarTransactionId())
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
                        .collect(Collectors.toList()))
                .build();

        if (txn.getStellarTransactions() == null) {
          txn.setStellarTransactions(List.of(stellarTransaction));
        } else {
          txn.getStellarTransactions().add(stellarTransaction);
        }
        txn.setTransferReceivedAt(parsePaymentTime(lastPayment.getCreatedAt()));
      }
    } catch (Exception ex) {
      errorEx("Failed to retrieve stellar transactions", ex);
    }

    if (request.getAmountIn() != null) {
      txn.setAmountIn(request.getAmountIn());
    }
    if (request.getAmountOut() != null) {
      txn.setAmountOut(request.getAmountOut());
    }
    if (request.getAmountFee() != null) {
      txn.setAmountFee(request.getAmountFee());
    }

    txn.setStellarTransactionId(request.getStellarTransactionId());
  }

  private List<ObservedPayment> getObservedPayments(String stellarTransactionId)
      throws IOException {
    return horizon
        .getServer()
        .payments()
        .includeTransactions(true)
        .forTransaction(stellarTransactionId)
        .execute()
        .getRecords()
        .stream()
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
                error("Failed to parse operation response");
              }
              return null;
            })
        .filter(Objects::nonNull)
        .sorted(Comparator.comparing(ObservedPayment::getCreatedAt))
        .collect(Collectors.toList());
  }
}

package org.stellar.anchor.platform.action.handlers;

import static org.stellar.anchor.api.sep.SepTransactionStatus.PENDING_ANCHOR;
import static org.stellar.anchor.api.sep.SepTransactionStatus.PENDING_EXTERNAL;
import static org.stellar.anchor.api.sep.SepTransactionStatus.PENDING_RECEIVER;
import static org.stellar.anchor.api.sep.SepTransactionStatus.PENDING_SENDER;
import static org.stellar.anchor.api.sep.SepTransactionStatus.PENDING_USR_TRANSFER_START;
import static org.stellar.anchor.platform.action.dto.ActionMethod.NOTIFY_ONCHAIN_FUNDS_RECEIVED;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import javax.validation.Validator;
import org.springframework.stereotype.Service;
import org.stellar.anchor.api.exception.AnchorException;
import org.stellar.anchor.api.platform.PlatformTransactionData.Kind;
import org.stellar.anchor.api.sep.SepTransactionStatus;
import org.stellar.anchor.asset.AssetService;
import org.stellar.anchor.platform.action.dto.ActionMethod;
import org.stellar.anchor.platform.action.dto.NotifyOnchainFundsReceivedRequest;
import org.stellar.anchor.platform.data.JdbcSep24Transaction;
import org.stellar.anchor.platform.data.JdbcSepTransaction;
import org.stellar.anchor.sep24.Sep24TransactionStore;
import org.stellar.anchor.sep31.Sep31TransactionStore;

@Service
public class NotifyOnchainFundsReceivedHandler
    extends ActionHandler<NotifyOnchainFundsReceivedRequest> {

  public NotifyOnchainFundsReceivedHandler(
      Sep24TransactionStore txn24Store,
      Sep31TransactionStore txn31Store,
      Validator validator,
      AssetService assetService) {
    super(txn24Store, txn31Store, validator, assetService);
  }

  @Override
  public ActionMethod getActionType() {
    return NOTIFY_ONCHAIN_FUNDS_RECEIVED;
  }

  @Override
  protected SepTransactionStatus getNextStatus(
      JdbcSepTransaction txn, NotifyOnchainFundsReceivedRequest request) {
    switch (txn.getProtocol()) {
      case "24":
        JdbcSep24Transaction txn24 = (JdbcSep24Transaction) txn;
        switch (Kind.from(txn24.getKind())) {
          case DEPOSIT:
            return PENDING_ANCHOR;
          default:
            throw new IllegalArgumentException(
                String.format(
                    "Invalid kind[%s] for protocol[%s] and action[%s]",
                    txn24.getKind(), txn24.getProtocol(), getActionType()));
        }
      case "31":
        return PENDING_RECEIVER;
      default:
        throw new IllegalArgumentException(
            String.format(
                "Invalid protocol[%s] for action[%s]", txn.getProtocol(), getActionType()));
    }
  }

  @Override
  protected Set<SepTransactionStatus> getSupportedStatuses(JdbcSepTransaction txn) {
    Set<SepTransactionStatus> supportedStatuses = new HashSet<>();

    switch (txn.getProtocol()) {
      case "24":
        JdbcSep24Transaction txn24 = (JdbcSep24Transaction) txn;
        switch (Kind.from(txn24.getKind())) {
          case DEPOSIT:
            supportedStatuses.add(PENDING_USR_TRANSFER_START);
            if (txn.getTransferReceivedAt() == null) {
              supportedStatuses.add(PENDING_EXTERNAL);
            }
        }
        break;
      case "31":
        supportedStatuses.add(PENDING_SENDER);
        break;
    }

    return supportedStatuses;
  }

  @Override
  protected Set<String> getSupportedProtocols() {
    return Set.of("24", "31");
  }

  @Override
  protected boolean isMessageRequired() {
    return false;
  }

  @Override
  protected void updateActionTransactionInfo(
      JdbcSepTransaction txn, NotifyOnchainFundsReceivedRequest request) throws AnchorException {
    // TODO: add Stellar Transactions
    if (txn.getStellarTransactionId() == null) {
      txn.setStellarTransactionId(request.getStellarTransactionId());
    } else if (txn.getTransferReceivedAt() == null) {
      if (request.getFundsReceivedAt() == null) {
        txn.setTransferReceivedAt(Instant.now());
      } else {
        txn.setTransferReceivedAt(request.getFundsReceivedAt());
      }
    }

    validateAsset("amount_in", request.getAmountIn());
    validateAsset("amount_out", request.getAmountOut());
    validateAsset("amount_fee", request.getAmountFee(), true);

    if (request.getAmountIn() != null) {
      txn.setAmountIn(request.getAmountIn().getAmount());
      txn.setAmountInAsset(request.getAmountIn().getAsset());
    }
    if (request.getAmountOut() != null) {
      txn.setAmountIn(request.getAmountOut().getAmount());
      txn.setAmountInAsset(request.getAmountOut().getAsset());
    }
    if (request.getAmountFee() != null) {
      txn.setAmountIn(request.getAmountFee().getAmount());
      txn.setAmountInAsset(request.getAmountFee().getAsset());
    }
  }
}

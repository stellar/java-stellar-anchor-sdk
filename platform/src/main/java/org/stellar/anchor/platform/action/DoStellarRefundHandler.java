package org.stellar.anchor.platform.action;

import static org.stellar.anchor.api.platform.PlatformTransactionData.Kind.WITHDRAWAL;
import static org.stellar.anchor.api.rpc.action.ActionMethod.DO_STELLAR_REFUND;
import static org.stellar.anchor.api.sep.SepTransactionStatus.PENDING_ANCHOR;
import static org.stellar.anchor.api.sep.SepTransactionStatus.PENDING_STELLAR;
import static org.stellar.anchor.util.MemoHelper.makeMemo;
import static org.stellar.anchor.util.MemoHelper.memoType;
import static org.stellar.anchor.util.SepHelper.memoTypeString;

import java.util.Set;
import javax.validation.Validator;
import org.stellar.anchor.api.exception.AnchorException;
import org.stellar.anchor.api.exception.SepException;
import org.stellar.anchor.api.exception.rpc.InvalidParamsException;
import org.stellar.anchor.api.exception.rpc.InvalidRequestException;
import org.stellar.anchor.api.platform.PlatformTransactionData.Kind;
import org.stellar.anchor.api.rpc.action.ActionMethod;
import org.stellar.anchor.api.rpc.action.DoStellarRefundRequest;
import org.stellar.anchor.api.sep.SepTransactionStatus;
import org.stellar.anchor.asset.AssetService;
import org.stellar.anchor.config.CustodyConfig;
import org.stellar.anchor.custody.CustodyService;
import org.stellar.anchor.horizon.Horizon;
import org.stellar.anchor.platform.data.JdbcSep24Transaction;
import org.stellar.anchor.platform.data.JdbcSepTransaction;
import org.stellar.anchor.sep24.Sep24TransactionStore;
import org.stellar.anchor.sep31.Sep31TransactionStore;
import org.stellar.sdk.Memo;

public class DoStellarRefundHandler extends ActionHandler<DoStellarRefundRequest> {

  private final CustodyService custodyService;
  private final CustodyConfig custodyConfig;

  public DoStellarRefundHandler(
      Sep24TransactionStore txn24Store,
      Sep31TransactionStore txn31Store,
      Validator validator,
      CustodyConfig custodyConfig,
      Horizon horizon,
      AssetService assetService,
      CustodyService custodyService) {
    super(txn24Store, txn31Store, validator, horizon, assetService, DoStellarRefundRequest.class);
    this.custodyService = custodyService;
    this.custodyConfig = custodyConfig;
  }

  @Override
  protected void validate(JdbcSepTransaction txn, DoStellarRefundRequest request)
      throws InvalidParamsException, InvalidRequestException {
    super.validate(txn, request);

    if (!custodyConfig.isCustodyIntegrationEnabled()) {
      throw new InvalidParamsException(
          String.format("Action[%s] requires enabled custody integration", getActionType()));
    }

    try {
      makeMemo(request.getMemo(), request.getMemoType());
    } catch (SepException e) {
      throw new InvalidParamsException(
          String.format("Invalid memo or memo_type: %s", e.getMessage()), e);
    }
  }

  @Override
  public ActionMethod getActionType() {
    return DO_STELLAR_REFUND;
  }

  @Override
  protected SepTransactionStatus getNextStatus(
      JdbcSepTransaction txn, DoStellarRefundRequest request) {
    return PENDING_STELLAR;
  }

  @Override
  protected Set<SepTransactionStatus> getSupportedStatuses(JdbcSepTransaction txn) {
    JdbcSep24Transaction txn24 = (JdbcSep24Transaction) txn;
    if (WITHDRAWAL == Kind.from(txn24.getKind())) {
      if (txn24.getTransferReceivedAt() != null) {
        return Set.of(PENDING_ANCHOR);
      }
    }
    return Set.of();
  }

  @Override
  protected Set<String> getSupportedProtocols() {
    return Set.of("24");
  }

  @Override
  protected void updateTransactionWithAction(JdbcSepTransaction txn, DoStellarRefundRequest request)
      throws AnchorException {
    JdbcSep24Transaction txn24 = (JdbcSep24Transaction) txn;

    Memo memo = makeMemo(request.getMemo(), request.getMemoType());
    if (memo != null) {
      txn24.setMemo(memo.toString());
      txn24.setMemoType(memoTypeString(memoType(memo)));
    }

    // TODO: Do we need to send request body?
    custodyService.createTransactionRefund(txn.getId(), null);
  }
}

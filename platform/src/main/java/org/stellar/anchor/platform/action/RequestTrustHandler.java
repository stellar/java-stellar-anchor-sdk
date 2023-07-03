package org.stellar.anchor.platform.action;

import static org.stellar.anchor.api.platform.PlatformTransactionData.Kind.DEPOSIT;
import static org.stellar.anchor.api.rpc.action.ActionMethod.REQUEST_TRUST;
import static org.stellar.anchor.api.sep.SepTransactionStatus.PENDING_ANCHOR;
import static org.stellar.anchor.api.sep.SepTransactionStatus.PENDING_TRUST;

import java.util.Set;
import javax.validation.Validator;
import org.springframework.stereotype.Service;
import org.stellar.anchor.api.exception.AnchorException;
import org.stellar.anchor.api.exception.BadRequestException;
import org.stellar.anchor.api.platform.PlatformTransactionData.Kind;
import org.stellar.anchor.api.rpc.action.ActionMethod;
import org.stellar.anchor.api.rpc.action.RequestTrustRequest;
import org.stellar.anchor.api.sep.SepTransactionStatus;
import org.stellar.anchor.asset.AssetService;
import org.stellar.anchor.config.CustodyConfig;
import org.stellar.anchor.horizon.Horizon;
import org.stellar.anchor.platform.data.JdbcSep24Transaction;
import org.stellar.anchor.platform.data.JdbcSepTransaction;
import org.stellar.anchor.sep24.Sep24TransactionStore;
import org.stellar.anchor.sep31.Sep31TransactionStore;

@Service
public class RequestTrustHandler extends ActionHandler<RequestTrustRequest> {

  private final CustodyConfig custodyConfig;

  public RequestTrustHandler(
      Sep24TransactionStore txn24Store,
      Sep31TransactionStore txn31Store,
      Validator validator,
      Horizon horizon,
      AssetService assetService,
      CustodyConfig custodyConfig) {
    super(txn24Store, txn31Store, validator, horizon, assetService);
    this.custodyConfig = custodyConfig;
  }

  @Override
  public void handle(Object requestParams) throws AnchorException {
    if (!custodyConfig.isCustodyIntegrationEnabled()) {
      throw new BadRequestException(
          String.format("Action[%s] requires disabled custody integration", getActionType()));
    }

    super.handle(requestParams);
  }

  @Override
  public ActionMethod getActionType() {
    return REQUEST_TRUST;
  }

  @Override
  protected SepTransactionStatus getNextStatus(
      JdbcSepTransaction txn, RequestTrustRequest request) {
    return PENDING_TRUST;
  }

  @Override
  protected Set<SepTransactionStatus> getSupportedStatuses(JdbcSepTransaction txn) {
    JdbcSep24Transaction txn24 = (JdbcSep24Transaction) txn;
    if (DEPOSIT == Kind.from(txn24.getKind())) {
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
  protected void updateTransactionWithAction(JdbcSepTransaction txn, RequestTrustRequest request) {}
}

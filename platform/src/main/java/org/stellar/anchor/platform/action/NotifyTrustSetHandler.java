package org.stellar.anchor.platform.action;

import static java.util.Collections.emptySet;
import static org.stellar.anchor.api.platform.PlatformTransactionData.Kind.DEPOSIT;
import static org.stellar.anchor.api.platform.PlatformTransactionData.Sep.SEP_24;
import static org.stellar.anchor.api.rpc.action.ActionMethod.NOTIFY_TRUST_SET;
import static org.stellar.anchor.api.sep.SepTransactionStatus.PENDING_ANCHOR;
import static org.stellar.anchor.api.sep.SepTransactionStatus.PENDING_TRUST;

import java.util.Set;
import org.stellar.anchor.api.exception.BadRequestException;
import org.stellar.anchor.api.exception.rpc.InvalidParamsException;
import org.stellar.anchor.api.exception.rpc.InvalidRequestException;
import org.stellar.anchor.api.platform.PlatformTransactionData.Kind;
import org.stellar.anchor.api.platform.PlatformTransactionData.Sep;
import org.stellar.anchor.api.rpc.action.ActionMethod;
import org.stellar.anchor.api.rpc.action.NotifyTrustSetRequest;
import org.stellar.anchor.api.sep.SepTransactionStatus;
import org.stellar.anchor.asset.AssetService;
import org.stellar.anchor.config.CustodyConfig;
import org.stellar.anchor.event.EventService;
import org.stellar.anchor.platform.data.JdbcSep24Transaction;
import org.stellar.anchor.platform.data.JdbcSepTransaction;
import org.stellar.anchor.platform.validator.RequestValidator;
import org.stellar.anchor.sep24.Sep24TransactionStore;
import org.stellar.anchor.sep31.Sep31TransactionStore;

public class NotifyTrustSetHandler extends ActionHandler<NotifyTrustSetRequest> {

  private final CustodyConfig custodyConfig;

  public NotifyTrustSetHandler(
      Sep24TransactionStore txn24Store,
      Sep31TransactionStore txn31Store,
      RequestValidator requestValidator,
      AssetService assetService,
      CustodyConfig custodyConfig,
      EventService eventService) {
    super(
        txn24Store,
        txn31Store,
        requestValidator,
        assetService,
        eventService,
        NotifyTrustSetRequest.class);
    this.custodyConfig = custodyConfig;
  }

  @Override
  protected void validate(JdbcSepTransaction txn, NotifyTrustSetRequest request)
      throws InvalidRequestException, InvalidParamsException, BadRequestException {
    super.validate(txn, request);

    if (custodyConfig.isCustodyIntegrationEnabled()) {
      throw new InvalidRequestException(
          String.format("Action[%s] requires disabled custody integration", getActionType()));
    }
  }

  @Override
  public ActionMethod getActionType() {
    return NOTIFY_TRUST_SET;
  }

  @Override
  protected SepTransactionStatus getNextStatus(
      JdbcSepTransaction txn, NotifyTrustSetRequest request) {
    return PENDING_ANCHOR;
  }

  @Override
  protected Set<SepTransactionStatus> getSupportedStatuses(JdbcSepTransaction txn) {
    if (SEP_24 == Sep.from(txn.getProtocol())) {
      JdbcSep24Transaction txn24 = (JdbcSep24Transaction) txn;
      if (DEPOSIT == Kind.from(txn24.getKind())) {
        return Set.of(PENDING_TRUST);
      }
    }
    return emptySet();
  }

  @Override
  protected void updateTransactionWithAction(
      JdbcSepTransaction txn, NotifyTrustSetRequest request) {}
}

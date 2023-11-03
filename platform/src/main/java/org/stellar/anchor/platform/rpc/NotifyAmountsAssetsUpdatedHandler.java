package org.stellar.anchor.platform.rpc;

import static java.util.Collections.emptySet;
import static org.stellar.anchor.api.platform.PlatformTransactionData.Sep.SEP_6;
import static org.stellar.anchor.api.rpc.method.RpcMethod.NOTIFY_AMOUNTS_ASSETS_UPDATED;
import static org.stellar.anchor.api.sep.SepTransactionStatus.*;

import java.util.Set;
import org.stellar.anchor.api.exception.AnchorException;
import org.stellar.anchor.api.exception.BadRequestException;
import org.stellar.anchor.api.exception.rpc.InvalidParamsException;
import org.stellar.anchor.api.exception.rpc.InvalidRequestException;
import org.stellar.anchor.api.platform.PlatformTransactionData.Sep;
import org.stellar.anchor.api.rpc.method.AmountAssetRequest;
import org.stellar.anchor.api.rpc.method.NotifyAmountsAssetsUpdatedRequest;
import org.stellar.anchor.api.rpc.method.RpcMethod;
import org.stellar.anchor.api.sep.SepTransactionStatus;
import org.stellar.anchor.asset.AssetService;
import org.stellar.anchor.event.EventService;
import org.stellar.anchor.metrics.MetricsService;
import org.stellar.anchor.platform.data.JdbcSepTransaction;
import org.stellar.anchor.platform.utils.AssetValidationUtils;
import org.stellar.anchor.platform.validator.RequestValidator;
import org.stellar.anchor.sep24.Sep24TransactionStore;
import org.stellar.anchor.sep31.Sep31TransactionStore;
import org.stellar.anchor.sep6.Sep6TransactionStore;

public class NotifyAmountsAssetsUpdatedHandler
    extends RpcMethodHandler<NotifyAmountsAssetsUpdatedRequest> {

  public NotifyAmountsAssetsUpdatedHandler(
      Sep6TransactionStore txn6Store,
      Sep24TransactionStore txn24Store,
      Sep31TransactionStore txn31Store,
      RequestValidator requestValidator,
      AssetService assetService,
      EventService eventService,
      MetricsService metricsService) {
    super(
        txn6Store,
        txn24Store,
        txn31Store,
        requestValidator,
        assetService,
        eventService,
        metricsService,
        NotifyAmountsAssetsUpdatedRequest.class);
  }

  @Override
  protected void validate(JdbcSepTransaction txn, NotifyAmountsAssetsUpdatedRequest request)
      throws BadRequestException, InvalidParamsException, InvalidRequestException {
    super.validate(txn, request);

    AssetValidationUtils.validateAsset(
        "amount_in",
        AmountAssetRequest.builder()
            .amount(request.getAmountIn().getAmount())
            .asset(request.getAmountIn().getAsset())
            .build(),
        assetService);

    AssetValidationUtils.validateAsset(
        "amount_out",
        AmountAssetRequest.builder()
            .amount(request.getAmountOut().getAmount())
            .asset(request.getAmountOut().getAsset())
            .build(),
        assetService);

    AssetValidationUtils.validateAsset(
        "amount_fee",
        AmountAssetRequest.builder()
            .amount(request.getAmountFee().getAmount())
            .asset(request.getAmountFee().getAsset())
            .build(),
        true,
        assetService);
  }

  @Override
  public RpcMethod getRpcMethod() {
    return NOTIFY_AMOUNTS_ASSETS_UPDATED;
  }

  @Override
  protected SepTransactionStatus getNextStatus(
      JdbcSepTransaction txn, NotifyAmountsAssetsUpdatedRequest request)
      throws InvalidRequestException, InvalidParamsException {
    return PENDING_ANCHOR;
  }

  @Override
  protected Set<SepTransactionStatus> getSupportedStatuses(JdbcSepTransaction txn) {
    if (Sep.from(txn.getProtocol()) == SEP_6) {
      return Set.of(INCOMPLETE, PENDING_ANCHOR, PENDING_CUSTOMER_INFO_UPDATE);
    }
    return emptySet();
  }

  @Override
  protected void updateTransactionWithRpcRequest(
      JdbcSepTransaction txn, NotifyAmountsAssetsUpdatedRequest request) throws AnchorException {
    txn.setAmountIn(request.getAmountIn().getAmount());
    txn.setAmountInAsset(request.getAmountIn().getAsset());

    txn.setAmountOut(request.getAmountOut().getAmount());
    txn.setAmountOutAsset(request.getAmountOut().getAsset());

    txn.setAmountFee(request.getAmountFee().getAmount());
    txn.setAmountFeeAsset(request.getAmountFee().getAsset());
  }
}

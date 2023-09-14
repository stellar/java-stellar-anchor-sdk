package org.stellar.anchor.platform.rpc;

import static org.stellar.anchor.api.platform.PlatformTransactionData.Kind.DEPOSIT;
import static org.stellar.anchor.api.platform.PlatformTransactionData.Sep.SEP_24;
import static org.stellar.anchor.api.rpc.method.RpcMethod.REQUEST_OFFCHAIN_FUNDS;
import static org.stellar.anchor.api.sep.SepTransactionStatus.INCOMPLETE;
import static org.stellar.anchor.api.sep.SepTransactionStatus.PENDING_ANCHOR;
import static org.stellar.anchor.api.sep.SepTransactionStatus.PENDING_USR_TRANSFER_START;

import java.util.HashSet;
import java.util.Set;
import org.stellar.anchor.api.exception.BadRequestException;
import org.stellar.anchor.api.exception.rpc.InvalidParamsException;
import org.stellar.anchor.api.exception.rpc.InvalidRequestException;
import org.stellar.anchor.api.platform.PlatformTransactionData.Kind;
import org.stellar.anchor.api.platform.PlatformTransactionData.Sep;
import org.stellar.anchor.api.rpc.method.AmountAssetRequest;
import org.stellar.anchor.api.rpc.method.RequestOffchainFundsRequest;
import org.stellar.anchor.api.rpc.method.RpcMethod;
import org.stellar.anchor.api.sep.SepTransactionStatus;
import org.stellar.anchor.asset.AssetService;
import org.stellar.anchor.event.EventService;
import org.stellar.anchor.platform.data.JdbcSep24Transaction;
import org.stellar.anchor.platform.data.JdbcSepTransaction;
import org.stellar.anchor.platform.utils.AssetValidationUtils;
import org.stellar.anchor.platform.validator.RequestValidator;
import org.stellar.anchor.sep24.Sep24TransactionStore;
import org.stellar.anchor.sep31.Sep31TransactionStore;

public class RequestOffchainFundsHandler extends RpcMethodHandler<RequestOffchainFundsRequest> {

  public RequestOffchainFundsHandler(
      Sep24TransactionStore txn24Store,
      Sep31TransactionStore txn31Store,
      RequestValidator requestValidator,
      AssetService assetService,
      EventService eventService) {
    super(
        txn24Store,
        txn31Store,
        requestValidator,
        assetService,
        eventService,
        RequestOffchainFundsRequest.class);
  }

  @Override
  protected void validate(JdbcSepTransaction txn, RequestOffchainFundsRequest request)
      throws InvalidParamsException, InvalidRequestException, BadRequestException {
    super.validate(txn, request);

    if (!((request.getAmountIn() == null
            && request.getAmountOut() == null
            && request.getAmountFee() == null
            && request.getAmountExpected() == null)
        || (request.getAmountIn() != null
            && request.getAmountOut() != null
            && request.getAmountFee() != null))) {
      throw new InvalidParamsException(
          "All or none of the amount_in, amount_out, and amount_fee should be set");
    }

    if (request.getAmountIn() != null) {
      if (AssetValidationUtils.isStellarAsset(request.getAmountIn().getAsset())) {
        throw new InvalidParamsException("amount_in.asset should be non-stellar asset");
      }
      AssetValidationUtils.validateAsset("amount_in", request.getAmountIn(), assetService);
    }
    if (request.getAmountOut() != null) {
      if (!AssetValidationUtils.isStellarAsset(request.getAmountOut().getAsset())) {
        throw new InvalidParamsException("amount_out.asset should be stellar asset");
      }
      AssetValidationUtils.validateAsset("amount_out", request.getAmountOut(), assetService);
    }
    if (request.getAmountFee() != null) {
      if (AssetValidationUtils.isStellarAsset(request.getAmountFee().getAsset())) {
        throw new InvalidParamsException("amount_fee.asset should be non-stellar asset");
      }
      AssetValidationUtils.validateAsset("amount_fee", request.getAmountFee(), true, assetService);
    }
    if (request.getAmountExpected() != null) {
      AssetValidationUtils.validateAsset(
          "amount_expected",
          AmountAssetRequest.builder()
              .amount(request.getAmountExpected().getAmount())
              .asset(request.getAmountIn().getAsset())
              .build(),
          assetService);
    }

    if (request.getAmountIn() == null && txn.getAmountIn() == null) {
      throw new InvalidParamsException("amount_in is required");
    }
    if (request.getAmountOut() == null && txn.getAmountOut() == null) {
      throw new InvalidParamsException("amount_out is required");
    }
    if (request.getAmountFee() == null && txn.getAmountFee() == null) {
      throw new InvalidParamsException("amount_fee is required");
    }
  }

  @Override
  public RpcMethod getRpcMethod() {
    return REQUEST_OFFCHAIN_FUNDS;
  }

  @Override
  protected SepTransactionStatus getNextStatus(
      JdbcSepTransaction txn, RequestOffchainFundsRequest request) {
    return PENDING_USR_TRANSFER_START;
  }

  @Override
  protected Set<SepTransactionStatus> getSupportedStatuses(JdbcSepTransaction txn) {
    Set<SepTransactionStatus> supportedStatuses = new HashSet<>();
    if (SEP_24 == Sep.from(txn.getProtocol())) {
      JdbcSep24Transaction txn24 = (JdbcSep24Transaction) txn;
      if (DEPOSIT == Kind.from(txn24.getKind())) {
        supportedStatuses.add(INCOMPLETE);
        if (!areFundsReceived(txn24)) {
          supportedStatuses.add(PENDING_ANCHOR);
        }
      }
    }
    return supportedStatuses;
  }

  @Override
  protected void updateTransactionWithRpcRequest(
      JdbcSepTransaction txn, RequestOffchainFundsRequest request) {
    if (request.getAmountIn() != null) {
      txn.setAmountIn(request.getAmountIn().getAmount());
      txn.setAmountInAsset(request.getAmountIn().getAsset());
    }
    if (request.getAmountOut() != null) {
      txn.setAmountOut(request.getAmountOut().getAmount());
      txn.setAmountOutAsset(request.getAmountOut().getAsset());
    }
    if (request.getAmountFee() != null) {
      txn.setAmountFee(request.getAmountFee().getAmount());
      txn.setAmountFeeAsset(request.getAmountFee().getAsset());
    }

    JdbcSep24Transaction txn24 = (JdbcSep24Transaction) txn;
    if (request.getAmountExpected() != null) {
      txn24.setAmountExpected(request.getAmountExpected().getAmount());
    } else if (request.getAmountIn() != null) {
      txn24.setAmountExpected(request.getAmountIn().getAmount());
    }
  }
}

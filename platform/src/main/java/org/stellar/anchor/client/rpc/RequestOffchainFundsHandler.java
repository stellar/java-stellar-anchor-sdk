package org.stellar.anchor.client.rpc;

import static org.stellar.anchor.api.platform.PlatformTransactionData.Kind.DEPOSIT;
import static org.stellar.anchor.api.platform.PlatformTransactionData.Kind.DEPOSIT_EXCHANGE;
import static org.stellar.anchor.api.rpc.method.RpcMethod.REQUEST_OFFCHAIN_FUNDS;
import static org.stellar.anchor.api.sep.SepTransactionStatus.*;

import com.google.common.collect.ImmutableSet;
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
import org.stellar.anchor.client.data.JdbcSep24Transaction;
import org.stellar.anchor.client.data.JdbcSep6Transaction;
import org.stellar.anchor.client.data.JdbcSepTransaction;
import org.stellar.anchor.client.utils.AssetValidationUtils;
import org.stellar.anchor.client.validator.RequestValidator;
import org.stellar.anchor.event.EventService;
import org.stellar.anchor.metrics.MetricsService;
import org.stellar.anchor.sep24.Sep24TransactionStore;
import org.stellar.anchor.sep31.Sep31TransactionStore;
import org.stellar.anchor.sep6.Sep6TransactionStore;

public class RequestOffchainFundsHandler extends RpcMethodHandler<RequestOffchainFundsRequest> {

  public RequestOffchainFundsHandler(
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
    switch (Sep.from((txn.getProtocol()))) {
      case SEP_6:
        JdbcSep6Transaction txn6 = (JdbcSep6Transaction) txn;
        if (ImmutableSet.of(DEPOSIT, DEPOSIT_EXCHANGE).contains(Kind.from(txn6.getKind()))) {
          supportedStatuses.add(INCOMPLETE);
          if (!areFundsReceived(txn6)) {
            supportedStatuses.add(PENDING_ANCHOR);
            supportedStatuses.add(PENDING_CUSTOMER_INFO_UPDATE);
          }
        }
        break;
      case SEP_24:
        JdbcSep24Transaction txn24 = (JdbcSep24Transaction) txn;
        if (DEPOSIT == Kind.from(txn24.getKind())) {
          supportedStatuses.add(INCOMPLETE);
          if (!areFundsReceived(txn24)) {
            supportedStatuses.add(PENDING_ANCHOR);
          }
        }
        break;
      default:
        break;
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
    switch (Sep.from(txn.getProtocol())) {
      case SEP_6:
        JdbcSep6Transaction txn6 = (JdbcSep6Transaction) txn;
        if (request.getAmountExpected() != null) {
          txn6.setAmountExpected(request.getAmountExpected().getAmount());
        } else if (request.getAmountIn() != null) {
          txn6.setAmountExpected(request.getAmountIn().getAmount());
        }
        if (request.getInstructions() != null) {
          txn6.setInstructions(request.getInstructions());
        }
        break;
      case SEP_24:
        JdbcSep24Transaction txn24 = (JdbcSep24Transaction) txn;
        if (request.getAmountExpected() != null) {
          txn24.setAmountExpected(request.getAmountExpected().getAmount());
        } else if (request.getAmountIn() != null) {
          txn24.setAmountExpected(request.getAmountIn().getAmount());
        }
        break;
      default:
        break;
    }
  }
}

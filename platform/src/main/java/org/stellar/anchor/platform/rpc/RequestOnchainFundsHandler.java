package org.stellar.anchor.platform.rpc;

import static org.stellar.anchor.api.platform.PlatformTransactionData.Kind.WITHDRAWAL;
import static org.stellar.anchor.api.platform.PlatformTransactionData.Sep.SEP_24;
import static org.stellar.anchor.api.rpc.method.RpcMethod.REQUEST_ONCHAIN_FUNDS;
import static org.stellar.anchor.api.sep.SepTransactionStatus.INCOMPLETE;
import static org.stellar.anchor.api.sep.SepTransactionStatus.PENDING_ANCHOR;
import static org.stellar.anchor.api.sep.SepTransactionStatus.PENDING_USR_TRANSFER_START;
import static org.stellar.anchor.util.MemoHelper.makeMemo;
import static org.stellar.anchor.util.MemoHelper.memoType;
import static org.stellar.anchor.util.SepHelper.memoTypeString;

import java.util.HashSet;
import java.util.Set;
import org.stellar.anchor.api.exception.AnchorException;
import org.stellar.anchor.api.exception.BadRequestException;
import org.stellar.anchor.api.exception.SepException;
import org.stellar.anchor.api.exception.rpc.InvalidParamsException;
import org.stellar.anchor.api.exception.rpc.InvalidRequestException;
import org.stellar.anchor.api.platform.PlatformTransactionData.Kind;
import org.stellar.anchor.api.platform.PlatformTransactionData.Sep;
import org.stellar.anchor.api.rpc.method.AmountAssetRequest;
import org.stellar.anchor.api.rpc.method.RequestOnchainFundsRequest;
import org.stellar.anchor.api.rpc.method.RpcMethod;
import org.stellar.anchor.api.sep.SepTransactionStatus;
import org.stellar.anchor.api.shared.SepDepositInfo;
import org.stellar.anchor.asset.AssetService;
import org.stellar.anchor.config.CustodyConfig;
import org.stellar.anchor.custody.CustodyService;
import org.stellar.anchor.event.EventService;
import org.stellar.anchor.platform.data.JdbcSep24Transaction;
import org.stellar.anchor.platform.data.JdbcSepTransaction;
import org.stellar.anchor.platform.service.Sep24DepositInfoNoneGenerator;
import org.stellar.anchor.platform.utils.AssetValidationUtils;
import org.stellar.anchor.platform.validator.RequestValidator;
import org.stellar.anchor.sep24.Sep24DepositInfoGenerator;
import org.stellar.anchor.sep24.Sep24TransactionStore;
import org.stellar.anchor.sep31.Sep31TransactionStore;
import org.stellar.anchor.util.CustodyUtils;
import org.stellar.sdk.Memo;

public class RequestOnchainFundsHandler extends RpcMethodHandler<RequestOnchainFundsRequest> {

  private final CustodyService custodyService;
  private final CustodyConfig custodyConfig;
  private final Sep24DepositInfoGenerator sep24DepositInfoGenerator;

  public RequestOnchainFundsHandler(
      Sep24TransactionStore txn24Store,
      Sep31TransactionStore txn31Store,
      RequestValidator requestValidator,
      AssetService assetService,
      CustodyService custodyService,
      CustodyConfig custodyConfig,
      Sep24DepositInfoGenerator sep24DepositInfoGenerator,
      EventService eventService) {
    super(
        txn24Store,
        txn31Store,
        requestValidator,
        assetService,
        eventService,
        RequestOnchainFundsRequest.class);
    this.custodyService = custodyService;
    this.custodyConfig = custodyConfig;
    this.sep24DepositInfoGenerator = sep24DepositInfoGenerator;
  }

  @Override
  protected void validate(JdbcSepTransaction txn, RequestOnchainFundsRequest request)
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
      if (!AssetValidationUtils.isStellarAsset(request.getAmountIn().getAsset())) {
        throw new InvalidParamsException("amount_in.asset should be stellar asset");
      }
      AssetValidationUtils.validateAsset("amount_in", request.getAmountIn(), assetService);
    }
    if (request.getAmountOut() != null) {
      if (AssetValidationUtils.isStellarAsset(request.getAmountOut().getAsset())) {
        throw new InvalidParamsException("amount_out.asset should be non-stellar asset");
      }
      AssetValidationUtils.validateAsset("amount_out", request.getAmountOut(), assetService);
    }
    if (request.getAmountFee() != null) {
      if (!AssetValidationUtils.isStellarAsset(request.getAmountFee().getAsset())) {
        throw new InvalidParamsException("amount_fee.asset should be stellar asset");
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

    if (sep24DepositInfoGenerator instanceof Sep24DepositInfoNoneGenerator) {
      Memo memo;
      try {
        memo = makeMemo(request.getMemo(), request.getMemoType());
      } catch (SepException e) {
        throw new InvalidParamsException(
            String.format("Invalid memo or memo_type: %s", e.getMessage()), e);
      }

      if (memo == null) {
        throw new InvalidParamsException("memo and memo_type are required");
      }
      if (request.getDestinationAccount() == null) {
        throw new InvalidParamsException("destination_account is required");
      }
    } else if (request.getMemo() != null
        || request.getMemoType() != null
        || request.getDestinationAccount() != null) {
      throw new InvalidParamsException(
          "Anchor is not configured to accept memo, memo_type and destination_account");
    }
  }

  @Override
  public RpcMethod getRpcMethod() {
    return REQUEST_ONCHAIN_FUNDS;
  }

  @Override
  protected SepTransactionStatus getNextStatus(
      JdbcSepTransaction txn, RequestOnchainFundsRequest request) {
    return PENDING_USR_TRANSFER_START;
  }

  @Override
  protected Set<SepTransactionStatus> getSupportedStatuses(JdbcSepTransaction txn) {
    Set<SepTransactionStatus> supportedStatuses = new HashSet<>();
    if (SEP_24 == Sep.from(txn.getProtocol())) {
      JdbcSep24Transaction txn24 = (JdbcSep24Transaction) txn;
      if (WITHDRAWAL == Kind.from(txn24.getKind())) {
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
      JdbcSepTransaction txn, RequestOnchainFundsRequest request) throws AnchorException {
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

    if (sep24DepositInfoGenerator instanceof Sep24DepositInfoNoneGenerator) {
      Memo memo = makeMemo(request.getMemo(), request.getMemoType());
      if (memo != null) {
        txn24.setMemo(memo.toString());
        txn24.setMemoType(memoTypeString(memoType(memo)));
      }
      txn24.setWithdrawAnchorAccount(request.getDestinationAccount());
      txn24.setToAccount(request.getDestinationAccount());
    } else {
      SepDepositInfo sep24DepositInfo = sep24DepositInfoGenerator.generate(txn24);
      txn24.setToAccount(sep24DepositInfo.getStellarAddress());
      txn24.setWithdrawAnchorAccount(sep24DepositInfo.getStellarAddress());
      txn24.setMemo(sep24DepositInfo.getMemo());
      txn24.setMemoType(sep24DepositInfo.getMemoType());
    }

    if (!CustodyUtils.isMemoTypeSupported(custodyConfig.getType(), txn24.getMemoType())) {
      throw new InvalidParamsException(
          String.format(
              "Memo type[%s] is not supported for custody type[%s]",
              txn24.getMemoType(), custodyConfig.getType()));
    }

    if (custodyConfig.isCustodyIntegrationEnabled()) {
      custodyService.createTransaction(txn24);
    }
  }
}

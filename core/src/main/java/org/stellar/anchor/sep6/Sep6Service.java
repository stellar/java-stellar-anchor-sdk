package org.stellar.anchor.sep6;

import static org.stellar.anchor.util.MemoHelper.*;
import static org.stellar.sdk.xdr.MemoType.MEMO_HASH;

import com.google.common.collect.ImmutableMap;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import org.stellar.anchor.api.event.AnchorEvent;
import org.stellar.anchor.api.exception.*;
import org.stellar.anchor.api.sep.AssetInfo;
import org.stellar.anchor.api.sep.SepTransactionStatus;
import org.stellar.anchor.api.sep.sep6.*;
import org.stellar.anchor.api.sep.sep6.InfoResponse.*;
import org.stellar.anchor.api.shared.RefundPayment;
import org.stellar.anchor.api.shared.Refunds;
import org.stellar.anchor.asset.AssetService;
import org.stellar.anchor.auth.Sep10Jwt;
import org.stellar.anchor.config.Sep6Config;
import org.stellar.anchor.event.EventService;
import org.stellar.anchor.util.SepHelper;
import org.stellar.anchor.util.TransactionHelper;
import org.stellar.sdk.Memo;

public class Sep6Service {
  private final Sep6Config sep6Config;
  private final AssetService assetService;
  private final RequestValidator requestValidator;
  private final Sep6TransactionStore txnStore;
  private final ExchangeAmountsCalculator exchangeAmountsCalculator;
  private final EventService.Session eventSession;

  private final InfoResponse infoResponse;

  public Sep6Service(
      Sep6Config sep6Config,
      AssetService assetService,
      RequestValidator requestValidator,
      Sep6TransactionStore txnStore,
      ExchangeAmountsCalculator exchangeAmountsCalculator,
      EventService eventService) {
    this.sep6Config = sep6Config;
    this.assetService = assetService;
    this.requestValidator = requestValidator;
    this.txnStore = txnStore;
    this.exchangeAmountsCalculator = exchangeAmountsCalculator;
    this.eventSession =
        eventService.createSession(this.getClass().getName(), EventService.EventQueue.TRANSACTION);
    this.infoResponse = buildInfoResponse();
  }

  public InfoResponse getInfo() {
    return infoResponse;
  }

  public StartDepositResponse deposit(Sep10Jwt token, StartDepositRequest request)
      throws AnchorException {
    // Pre-validation
    if (token == null) {
      throw new SepNotAuthorizedException("missing token");
    }
    if (request == null) {
      throw new SepValidationException("missing request");
    }

    AssetInfo asset = requestValidator.getDepositAsset(request.getAssetCode());
    if (request.getType() != null) {
      requestValidator.validateTypes(
          request.getType(), asset.getCode(), asset.getDeposit().getMethods());
    }
    if (request.getAmount() != null) {
      requestValidator.validateAmount(
          request.getAmount(),
          asset.getCode(),
          asset.getSignificantDecimals(),
          asset.getDeposit().getMinAmount(),
          asset.getDeposit().getMaxAmount());
    }
    requestValidator.validateAccount(request.getAccount());

    Memo memo = makeMemo(request.getMemo(), request.getMemoType());
    String id = SepHelper.generateSepTransactionId();

    Sep6TransactionBuilder builder =
        new Sep6TransactionBuilder(txnStore)
            .id(id)
            .transactionId(id)
            .status(SepTransactionStatus.INCOMPLETE.toString())
            .kind(Sep6Transaction.Kind.DEPOSIT.toString())
            .type(request.getType())
            .assetCode(request.getAssetCode())
            .assetIssuer(asset.getIssuer())
            .amountExpected(request.getAmount())
            .startedAt(Instant.now())
            .sep10Account(token.getAccount())
            .sep10AccountMemo(token.getAccountMemo())
            .toAccount(request.getAccount());

    if (memo != null) {
      builder.memo(memo.toString());
      builder.memoType(SepHelper.memoTypeString(memoType(memo)));
    }

    Sep6Transaction txn = builder.build();
    txnStore.save(txn);

    eventSession.publish(
        AnchorEvent.builder()
            .id(UUID.randomUUID().toString())
            .sep("6")
            .type(AnchorEvent.Type.TRANSACTION_CREATED)
            .transaction(TransactionHelper.toGetTransactionResponse(txn, assetService))
            .build());

    return StartDepositResponse.builder()
        .how("Check the transaction for more information about how to deposit.")
        .id(txn.getId())
        .build();
  }

  public StartDepositResponse depositExchange(Sep10Jwt token, StartDepositExchangeRequest request)
      throws AnchorException {
    if (token == null) {
      throw new SepNotAuthorizedException("missing token");
    }
    if (request == null) {
      throw new SepValidationException("missing request");
    }

    AssetInfo sellAsset = assetService.getAssetByName(request.getSourceAsset());
    if (sellAsset == null) {
      throw new SepValidationException(
          String.format("invalid operation for asset %s", request.getSourceAsset()));
    }

    AssetInfo buyAsset = requestValidator.getDepositAsset(request.getDestinationAsset());
    requestValidator.validateTypes(
        request.getType(), buyAsset.getCode(), buyAsset.getDeposit().getMethods());
    requestValidator.validateAmount(
        request.getAmount(),
        buyAsset.getCode(),
        buyAsset.getSignificantDecimals(),
        buyAsset.getDeposit().getMinAmount(),
        buyAsset.getDeposit().getMaxAmount());
    requestValidator.validateAccount(request.getAccount());

    ExchangeAmountsCalculator.Amounts amounts;
    if (request.getQuoteId() != null) {
      amounts =
          exchangeAmountsCalculator.calculateFromQuote(
              request.getQuoteId(), sellAsset, request.getAmount());
    } else {
      amounts =
          exchangeAmountsCalculator.calculate(
              buyAsset, sellAsset, request.getAmount(), token.getAccount());
    }

    Memo memo = makeMemo(request.getMemo(), request.getMemoType());
    String id = SepHelper.generateSepTransactionId();

    Sep6TransactionBuilder builder =
        new Sep6TransactionBuilder(txnStore)
            .id(id)
            .transactionId(id)
            .status(SepTransactionStatus.INCOMPLETE.toString())
            .kind(Sep6Transaction.Kind.DEPOSIT_EXCHANGE.toString())
            .type(request.getType())
            .assetCode(buyAsset.getCode())
            .assetIssuer(buyAsset.getIssuer())
            .amountIn(amounts.getAmountIn())
            .amountInAsset(amounts.getAmountInAsset())
            .amountOut(amounts.getAmountOut())
            .amountOutAsset(amounts.getAmountOutAsset())
            .amountFee(amounts.getAmountFee())
            .amountFeeAsset(amounts.getAmountFeeAsset())
            .amountExpected(request.getAmount())
            .amountExpected(request.getAmount())
            .startedAt(Instant.now())
            .sep10Account(token.getAccount())
            .sep10AccountMemo(token.getAccountMemo())
            .toAccount(request.getAccount())
            .quoteId(request.getQuoteId());

    if (memo != null) {
      builder.memo(memo.toString());
      builder.memoType(SepHelper.memoTypeString(memoType(memo)));
    }

    Sep6Transaction txn = builder.build();
    txnStore.save(txn);

    eventSession.publish(
        AnchorEvent.builder()
            .id(UUID.randomUUID().toString())
            .sep("6")
            .type(AnchorEvent.Type.TRANSACTION_CREATED)
            .transaction(TransactionHelper.toGetTransactionResponse(txn, assetService))
            .build());

    return StartDepositResponse.builder()
        .how("Check the transaction for more information about how to deposit.")
        .id(id)
        .build();
  }

  public StartWithdrawResponse withdraw(Sep10Jwt token, StartWithdrawRequest request)
      throws AnchorException {
    // Pre-validation
    if (token == null) {
      throw new SepNotAuthorizedException("missing token");
    }
    if (request == null) {
      throw new SepValidationException("missing request");
    }

    AssetInfo asset = requestValidator.getWithdrawAsset(request.getAssetCode());
    if (request.getType() != null) {
      requestValidator.validateTypes(
          request.getType(), asset.getCode(), asset.getWithdraw().getMethods());
    }
    if (request.getAmount() != null) {
      requestValidator.validateAmount(
          request.getAmount(),
          asset.getCode(),
          asset.getSignificantDecimals(),
          asset.getWithdraw().getMinAmount(),
          asset.getWithdraw().getMaxAmount());
    }

    String id = SepHelper.generateSepTransactionId();

    Sep6TransactionBuilder builder =
        new Sep6TransactionBuilder(txnStore)
            .id(id)
            .transactionId(id)
            .status(SepTransactionStatus.INCOMPLETE.toString())
            .kind(Sep6Transaction.Kind.WITHDRAWAL.toString())
            .type(request.getType())
            .assetCode(request.getAssetCode())
            .assetIssuer(asset.getIssuer())
            .amountExpected(request.getAmount())
            .startedAt(Instant.now())
            .sep10Account(token.getAccount())
            .sep10AccountMemo(token.getAccountMemo())
            .memo(generateMemo(id))
            .memoType(memoTypeAsString(MEMO_HASH))
            .fromAccount(token.getAccount())
            .withdrawAnchorAccount(asset.getDistributionAccount())
            .refundMemo(request.getRefundMemo())
            .refundMemoType(request.getRefundMemoType());

    Sep6Transaction txn = builder.build();
    txnStore.save(txn);

    eventSession.publish(
        AnchorEvent.builder()
            .id(UUID.randomUUID().toString())
            .sep("6")
            .type(AnchorEvent.Type.TRANSACTION_CREATED)
            .transaction(TransactionHelper.toGetTransactionResponse(txn, assetService))
            .build());

    return StartWithdrawResponse.builder()
        .accountId(asset.getDistributionAccount())
        .id(txn.getId())
        .memo(txn.getMemo())
        .memoType(memoTypeAsString(MEMO_HASH))
        .build();
  }

  public StartWithdrawResponse withdrawExchange(
      Sep10Jwt token, StartWithdrawExchangeRequest request) throws AnchorException {
    // Pre-validation
    if (token == null) {
      throw new SepNotAuthorizedException("missing token");
    }
    if (request == null) {
      throw new SepValidationException("missing request");
    }

    AssetInfo buyAsset = assetService.getAssetByName(request.getDestinationAsset());
    if (buyAsset == null) {
      throw new SepValidationException(
          String.format("invalid operation for asset %s", request.getDestinationAsset()));
    }

    AssetInfo sellAsset = requestValidator.getWithdrawAsset(request.getSourceAsset());
    requestValidator.validateTypes(
        request.getType(), sellAsset.getCode(), sellAsset.getWithdraw().getMethods());
    requestValidator.validateAmount(
        request.getAmount(),
        sellAsset.getCode(),
        sellAsset.getSignificantDecimals(),
        sellAsset.getWithdraw().getMinAmount(),
        sellAsset.getWithdraw().getMaxAmount());

    String id = SepHelper.generateSepTransactionId();

    ExchangeAmountsCalculator.Amounts amounts;
    if (request.getQuoteId() != null) {
      amounts =
          exchangeAmountsCalculator.calculateFromQuote(
              request.getQuoteId(), sellAsset, request.getAmount());
    } else {
      amounts =
          exchangeAmountsCalculator.calculate(
              buyAsset, sellAsset, request.getAmount(), token.getAccount());
    }

    Sep6TransactionBuilder builder =
        new Sep6TransactionBuilder(txnStore)
            .id(id)
            .transactionId(id)
            .status(SepTransactionStatus.INCOMPLETE.toString())
            .kind(Sep6Transaction.Kind.WITHDRAWAL_EXCHANGE.toString())
            .type(request.getType())
            .assetCode(sellAsset.getCode())
            .assetIssuer(sellAsset.getIssuer())
            .amountIn(amounts.getAmountIn())
            .amountInAsset(amounts.getAmountInAsset())
            .amountOut(amounts.getAmountOut())
            .amountOutAsset(amounts.getAmountOutAsset())
            .amountFee(amounts.getAmountFee())
            .amountFeeAsset(amounts.getAmountFeeAsset())
            .amountExpected(request.getAmount())
            .startedAt(Instant.now())
            .sep10Account(token.getAccount())
            .sep10AccountMemo(token.getAccountMemo())
            .memo(generateMemo(id))
            .memoType(memoTypeAsString(MEMO_HASH))
            .fromAccount(token.getAccount())
            .withdrawAnchorAccount(sellAsset.getDistributionAccount())
            .refundMemo(request.getRefundMemo())
            .refundMemoType(request.getRefundMemoType())
            .quoteId(request.getQuoteId());

    Sep6Transaction txn = builder.build();
    txnStore.save(txn);

    eventSession.publish(
        AnchorEvent.builder()
            .id(UUID.randomUUID().toString())
            .sep("6")
            .type(AnchorEvent.Type.TRANSACTION_CREATED)
            .transaction(TransactionHelper.toGetTransactionResponse(txn, assetService))
            .build());

    return StartWithdrawResponse.builder()
        .accountId(sellAsset.getDistributionAccount())
        .id(txn.getId())
        .memo(txn.getMemo())
        .memoType(memoTypeAsString(MEMO_HASH))
        .build();
  }

  public GetTransactionsResponse findTransactions(Sep10Jwt token, GetTransactionsRequest request)
      throws SepException {
    // Pre-validation
    if (token == null) {
      throw new SepNotAuthorizedException("missing token");
    }
    if (request == null) {
      throw new SepValidationException("missing request");
    }
    if (!request.getAccount().equals(token.getAccount())) {
      throw new SepNotAuthorizedException("account does not match token");
    }
    if (assetService.getAsset(request.getAssetCode()) == null) {
      throw new SepValidationException(
          String.format("asset code %s not supported", request.getAssetCode()));
    }

    // Query the transaction store
    List<Sep6Transaction> transactions =
        txnStore.findTransactions(token.getAccount(), token.getAccountMemo(), request);
    List<org.stellar.anchor.api.sep.sep6.Sep6Transaction> responses =
        transactions.stream().map(this::fromTxn).collect(Collectors.toList());

    return new GetTransactionsResponse(responses);
  }

  public GetTransactionResponse findTransaction(Sep10Jwt token, GetTransactionRequest request)
      throws AnchorException {
    // Pre-validation
    if (token == null) {
      throw new SepNotAuthorizedException("missing token");
    }
    if (request == null) {
      throw new SepValidationException("missing request");
    }

    // Query the transaction store
    Sep6Transaction txn;
    if (request.getId() != null) {
      txn = txnStore.findByTransactionId(request.getId());
    } else if (request.getStellarTransactionId() != null) {
      txn = txnStore.findByStellarTransactionId(request.getStellarTransactionId());
    } else if (request.getExternalTransactionId() != null) {
      txn = txnStore.findByExternalTransactionId(request.getExternalTransactionId());
    } else {
      throw new SepValidationException(
          "One of id, stellar_transaction_id, or external_transaction_id is required");
    }

    // Validate the transaction
    if (txn == null) {
      throw new NotFoundException("transaction not found");
    }
    if (!Objects.equals(txn.getSep10Account(), token.getAccount())) {
      throw new NotFoundException("account does not match token");
    }
    if (!Objects.equals(txn.getSep10AccountMemo(), token.getAccountMemo())) {
      throw new NotFoundException("account memo does not match token");
    }

    return new GetTransactionResponse(fromTxn(txn));
  }

  private org.stellar.anchor.api.sep.sep6.Sep6Transaction fromTxn(Sep6Transaction txn) {
    Refunds refunds = null;
    if (txn.getRefunds() != null && txn.getRefunds().getPayments() != null) {
      List<RefundPayment> payments = new ArrayList<>();
      for (RefundPayment payment : txn.getRefunds().getPayments()) {
        payments.add(
            RefundPayment.builder()
                .id(payment.getId())
                .idType(payment.getIdType())
                .amount(payment.getAmount())
                .fee(payment.getFee())
                .build());
      }
      refunds =
          Refunds.builder()
              .amountRefunded(txn.getRefunds().getAmountRefunded())
              .amountFee(txn.getRefunds().getAmountFee())
              .payments(payments.toArray(new RefundPayment[0]))
              .build();
    }
    org.stellar.anchor.api.sep.sep6.Sep6Transaction.Sep6TransactionBuilder builder =
        org.stellar.anchor.api.sep.sep6.Sep6Transaction.builder()
            .id(txn.getId())
            .kind(txn.getKind())
            .status(txn.getStatus())
            .statusEta(txn.getStatusEta())
            .moreInfoUrl(txn.getMoreInfoUrl())
            .amountIn(txn.getAmountIn())
            .amountInAsset(txn.getAmountInAsset())
            .amountOut(txn.getAmountOut())
            .amountOutAsset(txn.getAmountOutAsset())
            .amountFee(txn.getAmountFee())
            .amountFeeAsset(txn.getAmountFeeAsset())
            .startedAt(txn.getStartedAt().toString())
            .updatedAt(txn.getUpdatedAt().toString())
            .completedAt(txn.getCompletedAt() != null ? txn.getCompletedAt().toString() : null)
            .stellarTransactionId(txn.getStellarTransactionId())
            .externalTransactionId(txn.getExternalTransactionId())
            .from(txn.getFromAccount())
            .to(txn.getToAccount())
            .message(txn.getMessage())
            .refunds(refunds)
            .requiredInfoMessage(txn.getRequiredInfoMessage())
            .requiredInfoUpdates(txn.getRequiredInfoUpdates())
            .requiredCustomerInfoMessage(txn.getRequiredCustomerInfoMessage())
            .requiredCustomerInfoUpdates(txn.getRequiredCustomerInfoUpdates())
            .instructions(txn.getInstructions());

    if (org.stellar.anchor.sep6.Sep6Transaction.Kind.DEPOSIT.toString().equals(txn.getKind())) {
      return builder.depositMemo(txn.getMemo()).depositMemoType(txn.getMemoType()).build();
    } else {
      return builder
          .withdrawAnchorAccount(txn.getWithdrawAnchorAccount())
          .withdrawMemo(txn.getMemo())
          .withdrawMemoType(txn.getMemoType())
          .build();
    }
  }

  private InfoResponse buildInfoResponse() {
    InfoResponse response =
        InfoResponse.builder()
            .deposit(new HashMap<>())
            .depositExchange(new HashMap<>())
            .withdraw(new HashMap<>())
            .withdrawExchange(new HashMap<>())
            .fee(
                FeeResponse.builder()
                    .enabled(false)
                    .description("Fee endpoint is not supported.")
                    .build())
            .transactions(
                TransactionsResponse.builder().enabled(true).authenticationRequired(true).build())
            .transaction(
                TransactionResponse.builder().enabled(true).authenticationRequired(true).build())
            .features(
                FeaturesResponse.builder()
                    .accountCreation(sep6Config.getFeatures().isAccountCreation())
                    .claimableBalances(sep6Config.getFeatures().isClaimableBalances())
                    .build())
            .build();

    for (AssetInfo asset : assetService.listAllAssets()) {
      if (asset.getSep6Enabled()) {

        if (asset.getDeposit().getEnabled()) {
          List<String> methods = asset.getDeposit().getMethods();
          AssetInfo.Field type =
              AssetInfo.Field.builder()
                  .description("type of deposit to make")
                  .choices(methods)
                  .build();

          DepositAssetResponse deposit =
              DepositAssetResponse.builder()
                  .enabled(true)
                  .authenticationRequired(true)
                  .fields(ImmutableMap.of("type", type))
                  .build();

          response.getDeposit().put(asset.getCode(), deposit);
          response.getDepositExchange().put(asset.getCode(), deposit);
        }

        if (asset.getWithdraw().getEnabled()) {
          List<String> methods = asset.getWithdraw().getMethods();
          Map<String, WithdrawType> types = new HashMap<>();
          for (String method : methods) {
            types.put(method, WithdrawType.builder().fields(new HashMap<>()).build());
          }

          WithdrawAssetResponse withdraw =
              WithdrawAssetResponse.builder()
                  .enabled(true)
                  .authenticationRequired(true)
                  .types(types)
                  .build();

          response.getWithdraw().put(asset.getCode(), withdraw);
          response.getWithdrawExchange().put(asset.getCode(), withdraw);
        }
      }
    }
    return response;
  }
}

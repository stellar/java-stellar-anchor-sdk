package org.stellar.anchor.sep6;

import static io.micrometer.core.instrument.Metrics.counter;
import static org.stellar.anchor.util.MemoHelper.*;

import com.google.common.collect.ImmutableMap;
import io.micrometer.core.instrument.Counter;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.*;
import org.stellar.anchor.MoreInfoUrlConstructor;
import org.stellar.anchor.api.event.AnchorEvent;
import org.stellar.anchor.api.exception.*;
import org.stellar.anchor.api.sep.AssetInfo;
import org.stellar.anchor.api.sep.SepTransactionStatus;
import org.stellar.anchor.api.sep.sep6.*;
import org.stellar.anchor.api.sep.sep6.InfoResponse.*;
import org.stellar.anchor.api.shared.FeeDetails;
import org.stellar.anchor.asset.AssetService;
import org.stellar.anchor.auth.Sep10Jwt;
import org.stellar.anchor.client.ClientFinder;
import org.stellar.anchor.config.Sep6Config;
import org.stellar.anchor.event.EventService;
import org.stellar.anchor.sep6.ExchangeAmountsCalculator.Amounts;
import org.stellar.anchor.util.MetricConstants;
import org.stellar.anchor.util.SepHelper;
import org.stellar.anchor.util.TransactionHelper;
import org.stellar.sdk.Memo;

public class Sep6Service {
  private final Sep6Config sep6Config;
  private final AssetService assetService;
  private final RequestValidator requestValidator;
  private final ClientFinder clientFinder;
  private final Sep6TransactionStore txnStore;
  private final ExchangeAmountsCalculator exchangeAmountsCalculator;
  private final EventService.Session eventSession;
  private final InfoResponse infoResponse;
  private final MoreInfoUrlConstructor sep6MoreInfoUrlConstructor;
  private final Counter sep6TransactionRequestedCounter =
      counter(MetricConstants.SEP6_TRANSACTION_REQUESTED);
  private final Counter sep6TransactionQueriedCounter =
      counter(MetricConstants.SEP6_TRANSACTION_QUERIED);
  private final Counter sep6WithdrawalCounter =
      counter(
          MetricConstants.SEP6_TRANSACTION_CREATED,
          MetricConstants.TYPE,
          MetricConstants.TV_SEP6_WITHDRAWAL);
  private final Counter sep6WithdrawalExchangeCounter =
      counter(
          MetricConstants.SEP6_TRANSACTION_CREATED,
          MetricConstants.TYPE,
          MetricConstants.TV_SEP6_WITHDRAWAL_EXCHANGE);
  private final Counter sep6DepositCounter =
      counter(
          MetricConstants.SEP6_TRANSACTION_CREATED,
          MetricConstants.TYPE,
          MetricConstants.TV_SEP6_DEPOSIT);
  private final Counter sep6DepositExchangeCounter =
      counter(
          MetricConstants.SEP6_TRANSACTION_CREATED,
          MetricConstants.TYPE,
          MetricConstants.TV_SEP6_DEPOSIT_EXCHANGE);

  public Sep6Service(
      Sep6Config sep6Config,
      AssetService assetService,
      RequestValidator requestValidator,
      ClientFinder clientFinder,
      Sep6TransactionStore txnStore,
      ExchangeAmountsCalculator exchangeAmountsCalculator,
      EventService eventService,
      MoreInfoUrlConstructor sep6MoreInfoUrlConstructor) {
    this.sep6Config = sep6Config;
    this.assetService = assetService;
    this.requestValidator = requestValidator;
    this.clientFinder = clientFinder;
    this.txnStore = txnStore;
    this.exchangeAmountsCalculator = exchangeAmountsCalculator;
    this.eventSession =
        eventService.createSession(this.getClass().getName(), EventService.EventQueue.TRANSACTION);
    this.infoResponse = buildInfoResponse();
    this.sep6MoreInfoUrlConstructor = sep6MoreInfoUrlConstructor;
  }

  public InfoResponse getInfo() {
    return infoResponse;
  }

  public StartDepositResponse deposit(Sep10Jwt token, StartDepositRequest request)
      throws AnchorException {
    sep6TransactionRequestedCounter.increment();

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
            .toAccount(request.getAccount())
            .clientDomain(token.getClientDomain())
            .clientName(clientFinder.getClientName(token));

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

    sep6DepositCounter.increment();
    return StartDepositResponse.builder()
        .how("Check the transaction for more information about how to deposit.")
        .id(txn.getId())
        .build();
  }

  public StartDepositResponse depositExchange(Sep10Jwt token, StartDepositExchangeRequest request)
      throws AnchorException {
    sep6TransactionRequestedCounter.increment();

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

    Amounts amounts;
    if (request.getQuoteId() != null) {
      amounts =
          exchangeAmountsCalculator.calculateFromQuote(
              request.getQuoteId(), sellAsset, request.getAmount());
    } else {
      // If a quote is not provided, set the fee and out amounts to 0.
      // The business server should use the request_offchain_funds RPC to update the amounts.
      amounts =
          Amounts.builder()
              .amountIn(request.getAmount())
              .amountInAsset(sellAsset.getSep38AssetName())
              .amountOut("0")
              .amountOutAsset(buyAsset.getSep38AssetName())
              .feeDetails(new FeeDetails("0", sellAsset.getSep38AssetName(), null))
              .build();
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
            .feeDetails(amounts.feeDetails)
            .amountExpected(request.getAmount())
            .startedAt(Instant.now())
            .sep10Account(token.getAccount())
            .sep10AccountMemo(token.getAccountMemo())
            .toAccount(request.getAccount())
            .clientDomain(token.getClientDomain())
            .clientName(clientFinder.getClientName(token))
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

    sep6DepositExchangeCounter.increment();
    return StartDepositResponse.builder()
        .how("Check the transaction for more information about how to deposit.")
        .id(id)
        .build();
  }

  public StartWithdrawResponse withdraw(Sep10Jwt token, StartWithdrawRequest request)
      throws AnchorException {
    sep6TransactionRequestedCounter.increment();

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
    String sourceAccount = request.getAccount() != null ? request.getAccount() : token.getAccount();
    requestValidator.validateAccount(sourceAccount);

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
            .amountIn(request.getAmount())
            .amountInAsset(asset.getSep38AssetName())
            .amountExpected(request.getAmount())
            .startedAt(Instant.now())
            .sep10Account(token.getAccount())
            .sep10AccountMemo(token.getAccountMemo())
            .fromAccount(sourceAccount)
            .clientDomain(token.getClientDomain())
            .clientName(clientFinder.getClientName(token))
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

    sep6WithdrawalCounter.increment();
    return StartWithdrawResponse.builder().id(txn.getId()).build();
  }

  public StartWithdrawResponse withdrawExchange(
      Sep10Jwt token, StartWithdrawExchangeRequest request) throws AnchorException {
    sep6TransactionRequestedCounter.increment();

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
    String sourceAccount = request.getAccount() != null ? request.getAccount() : token.getAccount();
    requestValidator.validateAccount(sourceAccount);

    String id = SepHelper.generateSepTransactionId();

    Amounts amounts;
    if (request.getQuoteId() != null) {
      amounts =
          exchangeAmountsCalculator.calculateFromQuote(
              request.getQuoteId(), sellAsset, request.getAmount());
    } else {
      // If a quote is not provided, set the fee and out amounts to 0.
      // The business server should use the request_onchain_funds RPC to update the amounts.
      amounts =
          Amounts.builder()
              .amountIn(request.getAmount())
              .amountInAsset(sellAsset.getSep38AssetName())
              .amountOut("0")
              .amountOutAsset(buyAsset.getSep38AssetName())
              .feeDetails(new FeeDetails("0", sellAsset.getSep38AssetName(), null))
              .build();
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
            .feeDetails(amounts.getFeeDetails())
            .amountExpected(request.getAmount())
            .startedAt(Instant.now())
            .sep10Account(token.getAccount())
            .sep10AccountMemo(token.getAccountMemo())
            .fromAccount(sourceAccount)
            .clientDomain(token.getClientDomain())
            .clientName(clientFinder.getClientName(token))
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

    sep6WithdrawalExchangeCounter.increment();
    return StartWithdrawResponse.builder().id(txn.getId()).build();
  }

  public GetTransactionsResponse findTransactions(Sep10Jwt token, GetTransactionsRequest request)
      throws SepException, MalformedURLException, URISyntaxException {
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
    List<Sep6TransactionResponse> responses = new ArrayList<>();
    for (Sep6Transaction txn : transactions) {
      Sep6TransactionResponse tr = Sep6TransactionUtils.fromTxn(txn, sep6MoreInfoUrlConstructor);
      responses.add(tr);
    }

    sep6TransactionQueriedCounter.increment();
    return new GetTransactionsResponse(responses);
  }

  public GetTransactionResponse findTransaction(Sep10Jwt token, GetTransactionRequest request)
      throws AnchorException, MalformedURLException, URISyntaxException {
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

    sep6TransactionQueriedCounter.increment();
    return new GetTransactionResponse(
        Sep6TransactionUtils.fromTxn(txn, sep6MoreInfoUrlConstructor));
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
                  .minAmount(asset.getDeposit().getMinAmount())
                  .maxAmount(asset.getDeposit().getMaxAmount())
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
                  .minAmount(asset.getWithdraw().getMinAmount())
                  .maxAmount(asset.getWithdraw().getMaxAmount())
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

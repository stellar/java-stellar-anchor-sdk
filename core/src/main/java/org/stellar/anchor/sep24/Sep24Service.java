package org.stellar.anchor.sep24;

import static io.micrometer.core.instrument.Metrics.counter;
import static org.stellar.anchor.api.event.AnchorEvent.Type.TRANSACTION_CREATED;
import static org.stellar.anchor.api.sep.SepTransactionStatus.INCOMPLETE;
import static org.stellar.anchor.api.sep.sep24.InfoResponse.FeatureFlagResponse;
import static org.stellar.anchor.api.sep.sep24.InfoResponse.FeeResponse;
import static org.stellar.anchor.event.EventService.EventQueue.TRANSACTION;
import static org.stellar.anchor.sep24.Sep24Helper.fromTxn;
import static org.stellar.anchor.sep24.Sep24Transaction.Kind.*;
import static org.stellar.anchor.util.Log.debug;
import static org.stellar.anchor.util.Log.debugF;
import static org.stellar.anchor.util.Log.info;
import static org.stellar.anchor.util.Log.infoF;
import static org.stellar.anchor.util.Log.shorter;
import static org.stellar.anchor.util.MathHelper.decimal;
import static org.stellar.anchor.util.MemoHelper.makeMemo;
import static org.stellar.anchor.util.MemoHelper.memoType;
import static org.stellar.anchor.util.MetricConstants.*;
import static org.stellar.anchor.util.SepHelper.generateSepTransactionId;
import static org.stellar.anchor.util.SepHelper.memoTypeString;
import static org.stellar.anchor.util.SepLanguageHelper.validateLanguage;
import static org.stellar.anchor.util.StringHelper.isEmpty;

import io.micrometer.core.instrument.Counter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.*;
import org.stellar.anchor.api.event.AnchorEvent;
import org.stellar.anchor.api.exception.*;
import org.stellar.anchor.api.sep.AssetInfo;
import org.stellar.anchor.api.sep.sep24.GetTransactionRequest;
import org.stellar.anchor.api.sep.sep24.GetTransactionsRequest;
import org.stellar.anchor.api.sep.sep24.GetTransactionsResponse;
import org.stellar.anchor.api.sep.sep24.InfoResponse;
import org.stellar.anchor.api.sep.sep24.InteractiveTransactionResponse;
import org.stellar.anchor.api.sep.sep24.Sep24GetTransactionResponse;
import org.stellar.anchor.api.sep.sep24.TransactionResponse;
import org.stellar.anchor.asset.AssetService;
import org.stellar.anchor.auth.JwtService;
import org.stellar.anchor.auth.Sep10Jwt;
import org.stellar.anchor.config.AppConfig;
import org.stellar.anchor.config.ClientsConfig;
import org.stellar.anchor.config.CustodyConfig;
import org.stellar.anchor.config.Sep24Config;
import org.stellar.anchor.event.EventService;
import org.stellar.anchor.sep38.Sep38Quote;
import org.stellar.anchor.sep38.Sep38QuoteStore;
import org.stellar.anchor.util.ConfigHelper;
import org.stellar.anchor.util.CustodyUtils;
import org.stellar.anchor.util.MetricConstants;
import org.stellar.anchor.util.TransactionHelper;
import org.stellar.sdk.KeyPair;
import org.stellar.sdk.Memo;

public class Sep24Service {

  final AppConfig appConfig;
  final Sep24Config sep24Config;
  final ClientsConfig clientsConfig;
  final AssetService assetService;
  final JwtService jwtService;
  final Sep24TransactionStore txnStore;
  final EventService.Session eventSession;
  final InteractiveUrlConstructor interactiveUrlConstructor;
  final MoreInfoUrlConstructor moreInfoUrlConstructor;
  final CustodyConfig custodyConfig;

  final Sep38QuoteStore sep38QuoteStore;

  final Counter sep24TransactionRequestedCounter =
      counter(MetricConstants.SEP24_TRANSACTION_REQUESTED);
  final Counter sep24TransactionQueriedCounter = counter(MetricConstants.SEP24_TRANSACTION_QUERIED);
  final Counter sep24WithdrawalCounter =
      counter(
          MetricConstants.SEP24_TRANSACTION_CREATED,
          MetricConstants.TYPE,
          MetricConstants.TV_SEP24_WITHDRAWAL);
  final Counter sep24DepositCounter =
      counter(
          MetricConstants.SEP24_TRANSACTION_CREATED,
          MetricConstants.TYPE,
          MetricConstants.TV_SEP24_DEPOSIT);

  public static final List<String> INTERACTIVE_URL_JWT_REQUIRED_FIELDS_FROM_REQUEST =
      List.of("amount", "client_domain", "lang");
  public static String ERR_TOKEN_ACCOUNT_MISMATCH = "'account' does not match the one in the token";

  public Sep24Service(
      AppConfig appConfig,
      Sep24Config sep24Config,
      ClientsConfig clientsConfig,
      AssetService assetService,
      JwtService jwtService,
      Sep24TransactionStore txnStore,
      EventService eventService,
      InteractiveUrlConstructor interactiveUrlConstructor,
      MoreInfoUrlConstructor moreInfoUrlConstructor,
      CustodyConfig custodyConfig,
      Sep38QuoteStore sep38QuoteStore) {
    debug("appConfig:", appConfig);
    debug("sep24Config:", sep24Config);
    this.appConfig = appConfig;
    this.sep24Config = sep24Config;
    this.clientsConfig = clientsConfig;
    this.assetService = assetService;
    this.jwtService = jwtService;
    this.txnStore = txnStore;
    this.eventSession = eventService.createSession(this.getClass().getName(), TRANSACTION);
    this.interactiveUrlConstructor = interactiveUrlConstructor;
    this.moreInfoUrlConstructor = moreInfoUrlConstructor;
    this.custodyConfig = custodyConfig;
    this.sep38QuoteStore = sep38QuoteStore;
    info("Sep24Service initialized.");
  }

  public InteractiveTransactionResponse withdraw(
      Sep10Jwt token, Map<String, String> withdrawRequest)
      throws AnchorException, MalformedURLException, URISyntaxException {
    info("Creating withdrawal transaction.");
    // increment counter
    sep24TransactionRequestedCounter.increment();
    if (token == null) {
      info("missing SEP-10 token");
      throw new SepValidationException("missing token");
    }

    if (withdrawRequest == null) {
      info("missing withdraw request");
      throw new SepValidationException("no request");
    }

    infoF(
        "Sep24.withdraw. account={}, memo={}", shorter(token.getAccount()), token.getAccountMemo());
    String assetCode = withdrawRequest.get("asset_code");
    String assetIssuer = withdrawRequest.get("asset_issuer");
    String sourceAccount = withdrawRequest.get("account");
    String strAmount = withdrawRequest.get("amount");

    String lang = validateLanguage(appConfig, withdrawRequest.get("lang"));
    debugF("language: {}", lang);

    if (assetCode == null) {
      info("missing 'asset_code'");
      throw new SepValidationException("missing 'asset_code'");
    }

    if (isEmpty(sourceAccount)) {
      sourceAccount = token.getAccount();
    }

    // Verify that the asset code exists in our database, with withdraw enabled.
    AssetInfo asset = assetService.getAsset(assetCode, assetIssuer);
    debugF("Asset: {}", asset);
    if (asset == null || !asset.getWithdraw().getEnabled() || !asset.getSep24Enabled()) {
      infoF("invalid operation for asset {}", assetCode);
      throw new SepValidationException(String.format("invalid operation for asset %s", assetCode));
    }

    // Validate min amount
    Long minAmount = asset.getWithdraw().getMinAmount();
    if (strAmount != null && minAmount != null) {
      if (decimal(strAmount).compareTo(decimal(minAmount)) < 0) {
        infoF("invalid amount {}", strAmount);
        throw new SepValidationException(
            String.format("amount is less than asset's minimum limit: %s", strAmount));
      }
    }

    // Validate max amount
    Long maxAmount = asset.getWithdraw().getMaxAmount();
    if (strAmount != null && maxAmount != null) {
      if (decimal(strAmount).compareTo(decimal(maxAmount)) > 0) {
        infoF("invalid amount {}", strAmount);
        throw new SepValidationException(
            String.format("amount exceeds asset's maximum limit: %s", strAmount));
      }
    }

    // Validate sourceAccount
    try {
      debugF("checking if withdraw source account:{} is valid", sourceAccount);
      KeyPair.fromAccountId(sourceAccount);
    } catch (Exception ex) {
      infoF("invalid account format: {}", sourceAccount);
      throw new SepValidationException(String.format("invalid account: %s", sourceAccount), ex);
    }

    if (token.getClientDomain() != null)
      withdrawRequest.put("client_domain", token.getClientDomain());

    // TODO - jamie - should we be allowing user to specify memo? transaction are looked up
    // by PaymentObserver
    // by account+memo, could be collisions
    Memo memo = makeMemo(withdrawRequest.get("memo"), withdrawRequest.get("memo_type"));
    Memo refundMemo =
        makeMemo(withdrawRequest.get("refund_memo"), withdrawRequest.get("refund_memo_type"));
    String txnId = UUID.randomUUID().toString();
    Sep24TransactionBuilder builder =
        new Sep24TransactionBuilder(txnStore)
            .transactionId(txnId)
            .status(INCOMPLETE.toString())
            .kind(WITHDRAWAL.toString())
            .assetCode(assetCode)
            .assetIssuer(asset.getIssuer())
            .startedAt(Instant.now())
            .sep10Account(token.getAccount())
            .sep10AccountMemo(token.getAccountMemo())
            .fromAccount(sourceAccount)
            // TODO - jamie to add unique address generator
            .withdrawAnchorAccount(asset.getDistributionAccount())
            .toAccount(asset.getDistributionAccount())
            .clientDomain(token.getClientDomain());

    // TODO - jamie to look into memo vs withdrawal_memo
    if (memo != null) {
      debug("transaction memo detected.", memo);

      if (!CustodyUtils.isMemoTypeSupported(
          custodyConfig.getType(), memoTypeString(memoType(memo)))) {
        throw new SepValidationException(
            String.format(
                "Memo type[%s] is not supported for custody type[%s]",
                memoTypeString(memoType(memo)), custodyConfig.getType()));
      }

      builder.memo(memo.toString());
      builder.memoType(memoTypeString(memoType(memo)));
    }

    if (refundMemo != null) {
      debug("refund memo detected.", refundMemo);

      if (!CustodyUtils.isMemoTypeSupported(
          custodyConfig.getType(), memoTypeString(memoType(refundMemo)))) {
        throw new SepValidationException(
            String.format(
                "Refund memo type[%s] is not supported for custody type[%s]",
                memoTypeString(memoType(refundMemo)), custodyConfig.getType()));
      }

      builder.refundMemo(refundMemo.toString());
      builder.refundMemoType(memoTypeString(memoType(refundMemo)));
    }

    this.validatedAndPopulateQuote(
        builder,
        WITHDRAWAL.toString(),
        txnId,
        withdrawRequest.get("quote_id"),
        assetCode,
        assetIssuer,
        withdrawRequest.get("destination_asset"),
        strAmount);

    Sep24Transaction txn = builder.build();
    txnStore.save(txn);

    eventSession.publish(
        AnchorEvent.builder()
            .id(UUID.randomUUID().toString())
            .sep("24")
            .type(TRANSACTION_CREATED)
            .transaction(TransactionHelper.toGetTransactionResponse(txn, assetService))
            .build());

    infoF(
        "Saved withdraw transaction. from={}, amountIn={}, amountOut={}.",
        shorter(txn.getFromAccount()),
        txn.getAmountIn(),
        txn.getAmountOut());
    debug("Transaction details:", txn);
    InteractiveTransactionResponse response =
        new InteractiveTransactionResponse(
            "interactive_customer_info_needed",
            interactiveUrlConstructor.construct(txn, withdrawRequest, asset, token),
            txn.getTransactionId());

    // increment counter
    sep24WithdrawalCounter.increment();
    return response;
  }

  public InteractiveTransactionResponse deposit(Sep10Jwt token, Map<String, String> depositRequest)
      throws AnchorException, MalformedURLException, URISyntaxException {
    info("Creating deposit transaction.");
    counter(SEP24_TRANSACTION_REQUESTED, TYPE, TV_SEP24_DEPOSIT);
    if (token == null) {
      info("missing SEP-10 token");
      throw new SepValidationException("missing token");
    }

    if (depositRequest == null) {
      info("missing deposit request");
      throw new SepValidationException("no request");
    }

    infoF(
        "Sep24.deposit. account={}, memo={}", shorter(token.getAccount()), token.getAccountMemo());
    String assetCode = depositRequest.get("asset_code");
    String assetIssuer = depositRequest.get("asset_issuer");
    String destinationAccount = depositRequest.get("account");
    String strAmount = depositRequest.get("amount");

    String strClaimableSupported = depositRequest.get("claimable_balance_supported");
    boolean claimableSupported = false;
    if (strClaimableSupported != null) {
      claimableSupported = Boolean.parseBoolean(strClaimableSupported.toLowerCase(Locale.ROOT));
      debugF("claimable balance supported: {}", claimableSupported);
    }

    String lang = validateLanguage(appConfig, depositRequest.get("lang"));
    debugF("language: {}", lang);

    if (assetCode == null) {
      info("missing 'asset_code'");
      throw new SepValidationException("missing 'asset_code'");
    }

    if (isEmpty(destinationAccount)) {
      destinationAccount = token.getAccount();
    }

    if (!destinationAccount.equals(token.getAccount())) {
      ClientsConfig.ClientConfig clientConfig =
          ConfigHelper.getClientConfig(clientsConfig, token.getClientDomain(), token.getAccount());
      if (clientConfig != null && clientConfig.getDestinationAccounts() != null) {
        if (!clientConfig.getDestinationAccounts().contains(destinationAccount)) {
          infoF(
              "The request account:{} for wallet:{} is not in the allowed destination accounts list",
              destinationAccount,
              clientConfig.getName());
          throw new SepValidationException("Provided 'account' is not allowed");
        }
      } else {
        if (clientConfig == null || !clientConfig.isAllowAnyDestination()) {
          infoF(
              "The request account:{} does not match the one in the token:{}",
              destinationAccount,
              token.getAccount());
          throw new SepValidationException(ERR_TOKEN_ACCOUNT_MISMATCH);
        }
      }
    }

    // Verify that the asset code exists in our database, with withdraw enabled.
    AssetInfo asset = assetService.getAsset(assetCode, assetIssuer);
    if (asset == null || !asset.getDeposit().getEnabled() || !asset.getSep24Enabled()) {
      infoF("invalid operation for asset {}", assetCode);
      throw new SepValidationException(String.format("invalid operation for asset %s", assetCode));
    }

    // Validate min amount
    Long minAmount = asset.getWithdraw().getMinAmount();
    if (strAmount != null && minAmount != null) {
      if (decimal(strAmount).compareTo(decimal(minAmount)) < 0) {
        infoF("invalid amount {}", strAmount);
        throw new SepValidationException(
            String.format("amount is less than asset's minimum limit: %s", strAmount));
      }
    }

    // Validate max amount
    Long maxAmount = asset.getWithdraw().getMaxAmount();
    if (strAmount != null && maxAmount != null) {
      if (decimal(strAmount).compareTo(decimal(maxAmount)) > 0) {
        infoF("invalid amount {}", strAmount);
        throw new SepValidationException(
            String.format("amount exceeds asset's maximum limit: %s", strAmount));
      }
    }

    try {
      debugF("checking if deposit destination account:{} is valid", destinationAccount);
      KeyPair.fromAccountId(destinationAccount);
    } catch (Exception ex) {
      infoF("invalid account format: {}", destinationAccount);
      throw new SepValidationException(
          String.format("invalid account: %s", destinationAccount), ex);
    }

    if (token.getClientDomain() != null)
      depositRequest.put("client_domain", token.getClientDomain());

    Memo memo = makeMemo(depositRequest.get("memo"), depositRequest.get("memo_type"));
    String txnId = generateSepTransactionId();
    Sep24TransactionBuilder builder =
        new Sep24TransactionBuilder(txnStore)
            .transactionId(txnId)
            .status(INCOMPLETE.toString())
            .kind(DEPOSIT.toString())
            .assetCode(assetCode)
            .assetIssuer(depositRequest.get("asset_issuer"))
            .startedAt(Instant.now())
            .sep10Account(token.getAccount())
            .sep10AccountMemo(token.getAccountMemo())
            .toAccount(destinationAccount)
            .clientDomain(token.getClientDomain())
            .claimableBalanceSupported(claimableSupported);

    if (memo != null) {
      debug("transaction memo detected.", memo);

      if (!CustodyUtils.isMemoTypeSupported(
          custodyConfig.getType(), memoTypeString(memoType(memo)))) {
        throw new SepValidationException(
            String.format(
                "Memo type[%s] is not supported for custody type[%s]",
                memoTypeString(memoType(memo)), custodyConfig.getType()));
      }

      builder.memo(memo.toString());
      builder.memoType(memoTypeString(memoType(memo)));
    }

    this.validatedAndPopulateQuote(
        builder,
        DEPOSIT.toString(),
        txnId,
        depositRequest.get("quote_id"),
        assetCode,
        assetIssuer,
        depositRequest.get("source_asset"),
        strAmount);

    Sep24Transaction txn = builder.build();
    txnStore.save(txn);

    eventSession.publish(
        AnchorEvent.builder()
            .id(UUID.randomUUID().toString())
            .sep("24")
            .type(TRANSACTION_CREATED)
            .transaction(TransactionHelper.toGetTransactionResponse(txn, assetService))
            .build());

    infoF(
        "Saved deposit transaction. to={}, amountIn={}, amountOut={}.",
        shorter(txn.getToAccount()),
        txn.getAmountIn(),
        txn.getAmountOut());
    debug("Transaction details:", txn);

    InteractiveTransactionResponse response =
        new InteractiveTransactionResponse(
            "interactive_customer_info_needed",
            interactiveUrlConstructor.construct(txn, depositRequest, asset, token),
            txn.getTransactionId());
    // increment counter
    sep24DepositCounter.increment();
    return response;
  }

  public GetTransactionsResponse findTransactions(Sep10Jwt token, GetTransactionsRequest txReq)
      throws SepException, MalformedURLException, URISyntaxException {
    if (token == null) {
      info("missing SEP-10 token");
      throw new SepNotAuthorizedException("missing token");
    }

    String assetCode = txReq.getAssetCode();
    String assetIssuer = null;

    if (assetCode.contains("stellar:")) {
      String[] parsed = txReq.getAssetCode().split(":");
      if (parsed.length == 3) {
        assetCode = parsed[1];
        assetIssuer = parsed[2];
      } else if (parsed.length == 2) {
        assetCode = parsed[1];
      }
    }

    infoF("findTransactions. account={}", shorter(token.getAccount()));
    if (assetService.getAsset(assetCode, assetIssuer) == null) {
      infoF(
          "asset code:{} not supported",
          (txReq.getAssetCode() == null) ? "null" : txReq.getAssetCode());
      throw new SepValidationException("asset code not supported");
    }
    List<Sep24Transaction> txns =
        txnStore.findTransactions(token.getAccount(), token.getAccountMemo(), txReq);
    GetTransactionsResponse result = new GetTransactionsResponse();
    List<TransactionResponse> list = new ArrayList<>();
    debugF("found {} transactions", txns.size());
    for (Sep24Transaction txn : txns) {
      TransactionResponse transactionResponse = fromTxn(assetService, moreInfoUrlConstructor, txn);
      list.add(transactionResponse);
    }
    result.setTransactions(list);
    // increment counter
    sep24TransactionQueriedCounter.increment();
    return result;
  }

  public Sep24GetTransactionResponse findTransaction(Sep10Jwt token, GetTransactionRequest txReq)
      throws SepException, IOException, URISyntaxException {
    if (token == null) {
      info("missing SEP-10 token");
      throw new SepNotAuthorizedException("missing token");
    }

    if (txReq == null) {
      info("missing request object");
      throw new SepValidationException("missing request object");
    }

    infoF("findTransaction. account={}", shorter(token.getAccount()));

    Sep24Transaction txn;
    if (txReq.getId() != null) {
      infoF("id={}", txReq.getId());
      txn = txnStore.findByTransactionId(txReq.getId());
    } else if (txReq.getStellarTransactionId() != null) {
      infoF("stellarTransactionId={}", shorter(txReq.getStellarTransactionId()));
      txn = txnStore.findByStellarTransactionId(txReq.getStellarTransactionId());
    } else if (txReq.getExternalTransactionId() != null) {
      infoF("externalTransactionId={}", shorter(txReq.getExternalTransactionId()));
      txn = txnStore.findByExternalTransactionId(txReq.getExternalTransactionId());
    } else {
      throw new SepValidationException(
          "One of id, stellar_transaction_id or external_transaction_id is required.");
    }

    // We should not return the transaction that belongs to other accounts.
    if (txn == null || !txn.getSep10Account().equals(token.getAccount())) {
      infoF("no transactions found with account:{}", token.getAccount());
      throw new SepNotFoundException("transaction not found");
    }

    // If the token has a memo, make sure the transaction belongs to the account
    // with the same memo.
    if (token.getAccountMemo() != null
        && txn.getSep10Account().equals(token.getAccount() + ":" + token.getAccountMemo())) {
      infoF(
          "no transactions found with account:{} memo:{}",
          token.getAccount(),
          token.getAccountMemo());
      throw new SepNotFoundException("transaction not found");
    }
    // increment counter
    sep24TransactionQueriedCounter.increment();
    return Sep24GetTransactionResponse.of(fromTxn(assetService, moreInfoUrlConstructor, txn));
  }

  public InfoResponse getInfo() {
    info("Getting Sep24 info");
    List<AssetInfo> assets = assetService.listAllAssets();
    debugF("{} assets found", assets.size());

    Map<String, InfoResponse.OperationResponse> depositMap = new HashMap<>();
    Map<String, InfoResponse.OperationResponse> withdrawMap = new HashMap<>();
    for (AssetInfo asset : assets) {
      if (asset.getDeposit().getEnabled())
        depositMap.put(
            asset.getCode(), InfoResponse.OperationResponse.fromAssetOperation(asset.getDeposit()));
      if (asset.getWithdraw().getEnabled())
        withdrawMap.put(
            asset.getCode(),
            InfoResponse.OperationResponse.fromAssetOperation(asset.getWithdraw()));
    }

    return InfoResponse.builder()
        .deposit(depositMap)
        .withdraw(withdrawMap)
        .fee(new FeeResponse(false))
        .features(
            new FeatureFlagResponse(
                sep24Config.getFeatures().getAccountCreation(),
                sep24Config.getFeatures().getClaimableBalances()))
        .build();
  }

  private void validatedAndPopulateQuote(
      Sep24TransactionBuilder builder,
      String kind,
      String txnId,
      String quoteId,
      String assetCode,
      String assetIssuer,
      String sourceOrDestAsset,
      String strAmount)
      throws BadRequestException {
    if (quoteId == null) {
      return;
    }

    Sep38Quote quote = sep38QuoteStore.findByQuoteId(quoteId);
    if (quote == null) {
      infoF("Quote ({}) was not found", quoteId);
      throw new BadRequestException(String.format("quote(id=%s) was not found.", quoteId));
    }

    String[] onChainAsset =
        kind.equals(DEPOSIT.toString())
            ? quote.getBuyAsset().split(":")
            : quote.getSellAsset().split(":");
    String offChainAsset =
        kind.equals(DEPOSIT.toString()) ? quote.getSellAsset() : quote.getBuyAsset();

    // assetCode needs to match the on-chain asset code in the quote
    if (!assetCode.equals(onChainAsset[1])) {
      System.out.println(onChainAsset[0] + " " + assetCode);
      infoF("Quote ({}) does not match asset code ({})", quoteId, assetCode);
      throw new BadRequestException(
          String.format("quote(id=%s) does not match asset code (%s).", quoteId, assetCode));
    }

    // issuer, if provided, needs to match the on-chain asset issuer in the quote, except for native
    if (assetIssuer != null
        && (!assetIssuer.equals(onChainAsset[2]) || !assetCode.equals("native"))) {
      infoF("Quote ({}) does not match asset issuer ({})", quoteId, assetIssuer);
      throw new BadRequestException(
          String.format("quote(id=%s) does not match asset issuer (%s).", quoteId, assetIssuer));
    }

    // source or destination asset, if provided, needs to match the off-chain asset in the quote
    if (sourceOrDestAsset != null && !sourceOrDestAsset.equals(offChainAsset)) {
      infoF(
          "Quote ({}) does not match source or destination asset ({})", quoteId, sourceOrDestAsset);
      throw new BadRequestException(
          String.format(
              "quote(id=%s) does not match source or destination asset (%s).",
              quoteId, sourceOrDestAsset));
    }

    // amount, if provided, needs to match the sell_amount in the quote
    if (strAmount != null && !(decimal(strAmount).equals(decimal(quote.getSellAmount())))) {
      System.out.println(quote.getSellAmount() + " " + strAmount);
      infoF("Quote ({}) does not match source amount ({})", quoteId, strAmount);
      throw new BadRequestException(
          String.format("quote(id=%s) does not match amount (%s).", quoteId, strAmount));
    }

    debugF("Updating transaction ({}) with quote ({})", txnId, quoteId);
    builder.quoteId(quoteId);
    builder.amountExpected(quote.getSellAmount());
    if (kind.equals(DEPOSIT.toString())) {
      builder.sourceAsset(offChainAsset);
    } else {
      builder.destinationAsset(offChainAsset);
    }
  }
}

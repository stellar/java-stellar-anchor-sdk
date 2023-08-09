package org.stellar.anchor.sep24;

import static org.stellar.anchor.api.event.AnchorEvent.Type.TRANSACTION_CREATED;
import static org.stellar.anchor.api.sep.SepTransactionStatus.INCOMPLETE;
import static org.stellar.anchor.api.sep.sep24.InfoResponse.FeatureFlagResponse;
import static org.stellar.anchor.api.sep.sep24.InfoResponse.FeeResponse;
import static org.stellar.anchor.event.EventService.EventQueue.TRANSACTION;
import static org.stellar.anchor.sep24.Sep24Helper.setSharedTransactionResponseFields;
import static org.stellar.anchor.sep24.Sep24Transaction.Kind.WITHDRAWAL;
import static org.stellar.anchor.util.Log.debug;
import static org.stellar.anchor.util.Log.debugF;
import static org.stellar.anchor.util.Log.info;
import static org.stellar.anchor.util.Log.infoF;
import static org.stellar.anchor.util.Log.shorter;
import static org.stellar.anchor.util.MathHelper.decimal;
import static org.stellar.anchor.util.MemoHelper.makeMemo;
import static org.stellar.anchor.util.MemoHelper.memoType;
import static org.stellar.anchor.util.SepHelper.generateSepTransactionId;
import static org.stellar.anchor.util.SepHelper.memoTypeString;
import static org.stellar.anchor.util.SepLanguageHelper.validateLanguage;
import static org.stellar.anchor.util.StringHelper.isEmpty;

import com.google.gson.Gson;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.stellar.anchor.api.event.AnchorEvent;
import org.stellar.anchor.api.exception.AnchorException;
import org.stellar.anchor.api.exception.SepException;
import org.stellar.anchor.api.exception.SepNotAuthorizedException;
import org.stellar.anchor.api.exception.SepNotFoundException;
import org.stellar.anchor.api.exception.SepValidationException;
import org.stellar.anchor.api.sep.AssetInfo;
import org.stellar.anchor.api.sep.sep24.DepositTransactionResponse;
import org.stellar.anchor.api.sep.sep24.GetTransactionRequest;
import org.stellar.anchor.api.sep.sep24.GetTransactionsRequest;
import org.stellar.anchor.api.sep.sep24.GetTransactionsResponse;
import org.stellar.anchor.api.sep.sep24.InfoResponse;
import org.stellar.anchor.api.sep.sep24.InteractiveTransactionResponse;
import org.stellar.anchor.api.sep.sep24.Sep24GetTransactionResponse;
import org.stellar.anchor.api.sep.sep24.TransactionResponse;
import org.stellar.anchor.api.sep.sep24.WithdrawTransactionResponse;
import org.stellar.anchor.asset.AssetService;
import org.stellar.anchor.auth.JwtService;
import org.stellar.anchor.auth.Sep10Jwt;
import org.stellar.anchor.config.AppConfig;
import org.stellar.anchor.config.CustodyConfig;
import org.stellar.anchor.config.Sep24Config;
import org.stellar.anchor.event.EventService;
import org.stellar.anchor.util.CustodyUtils;
import org.stellar.anchor.util.GsonUtils;
import org.stellar.anchor.util.TransactionHelper;
import org.stellar.sdk.KeyPair;
import org.stellar.sdk.Memo;

public class Sep24Service {

  final AppConfig appConfig;
  final Sep24Config sep24Config;
  final AssetService assetService;
  final JwtService jwtService;
  final Sep24TransactionStore txnStore;
  final EventService.Session eventSession;
  final InteractiveUrlConstructor interactiveUrlConstructor;
  final MoreInfoUrlConstructor moreInfoUrlConstructor;
  final CustodyConfig custodyConfig;

  static final Gson gson = GsonUtils.getInstance();

  public static final List<String> INTERACTIVE_URL_JWT_REQUIRED_FIELDS_FROM_REQUEST =
      List.of("amount", "client_domain", "lang");

  public Sep24Service(
      AppConfig appConfig,
      Sep24Config sep24Config,
      AssetService assetService,
      JwtService jwtService,
      Sep24TransactionStore txnStore,
      EventService eventService,
      InteractiveUrlConstructor interactiveUrlConstructor,
      MoreInfoUrlConstructor moreInfoUrlConstructor,
      CustodyConfig custodyConfig) {
    debug("appConfig:", appConfig);
    debug("sep24Config:", sep24Config);
    this.appConfig = appConfig;
    this.sep24Config = sep24Config;
    this.assetService = assetService;
    this.jwtService = jwtService;
    this.txnStore = txnStore;
    this.eventSession = eventService.createSession(this.getClass().getName(), TRANSACTION);
    this.interactiveUrlConstructor = interactiveUrlConstructor;
    this.moreInfoUrlConstructor = moreInfoUrlConstructor;
    this.custodyConfig = custodyConfig;
    info("Sep24Service initialized.");
  }

  public InteractiveTransactionResponse withdraw(
      Sep10Jwt token, Map<String, String> withdrawRequest)
      throws AnchorException, MalformedURLException, URISyntaxException {
    info("Creating withdrawal transaction.");
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
    debug("language: {}", lang);

    if (assetCode == null) {
      info("missing 'asset_code'");
      throw new SepValidationException("missing 'asset_code'");
    }

    if (isEmpty(sourceAccount)) {
      sourceAccount = token.getAccount();
    }

    // Verify that the asset code exists in our database, with withdraw enabled.
    AssetInfo asset = assetService.getAsset(assetCode, assetIssuer);
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
    return new InteractiveTransactionResponse(
        "interactive_customer_info_needed",
        interactiveUrlConstructor.construct(txn, withdrawRequest),
        txn.getTransactionId());
  }

  public InteractiveTransactionResponse deposit(Sep10Jwt token, Map<String, String> depositRequest)
      throws AnchorException, MalformedURLException, URISyntaxException {
    info("Creating deposit transaction.");
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
    debug("language: {}", lang);

    if (assetCode == null) {
      info("missing 'asset_code'");
      throw new SepValidationException("missing 'asset_code'");
    }

    if (isEmpty(destinationAccount)) {
      destinationAccount = token.getAccount();
    }

    if (!destinationAccount.equals(token.getAccount())) {
      infoF(
          "The request account:{} does not match the one in the token:{}",
          destinationAccount,
          token.getAccount());
      throw new SepValidationException("'account' does not match the one in the token");
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
            .kind(Sep24Transaction.Kind.DEPOSIT.toString())
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
      builder.memo(memo.toString());
      builder.memoType(memoTypeString(memoType(memo)));
    }

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

    return new InteractiveTransactionResponse(
        "interactive_customer_info_needed",
        interactiveUrlConstructor.construct(txn, depositRequest),
        txn.getTransactionId());
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
      TransactionResponse transactionResponse = fromTxn(txn, txReq.getLang());
      list.add(transactionResponse);
    }
    result.setTransactions(list);

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

    return Sep24GetTransactionResponse.of(fromTxn(txn, txReq.getLang()));
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

  TransactionResponse fromTxn(Sep24Transaction txn, String lang)
      throws MalformedURLException, URISyntaxException, SepException {
    debugF(
        "Converting Sep24Transaction to Transaction Response. kind={}, transactionId={}, lang={}",
        txn.getTransactionId(),
        txn.getTransactionId(),
        lang);
    TransactionResponse response;
    if (txn.getKind().equals(Sep24Transaction.Kind.DEPOSIT.toString())) {
      response = fromDepositTxn(txn);
    } else if (txn.getKind().equals(WITHDRAWAL.toString())) {
      response = fromWithdrawTxn(txn);
    } else {
      throw new SepException(String.format("unsupported txn kind:%s", txn.getKind()));
    }

    // Calculate refund information.
    AssetInfo assetInfo =
        assetService.getAsset(txn.getRequestAssetCode(), txn.getRequestAssetIssuer());
    return Sep24Helper.updateRefundInfo(response, txn, assetInfo);
  }

  public TransactionResponse fromDepositTxn(Sep24Transaction txn)
      throws MalformedURLException, URISyntaxException {

    DepositTransactionResponse txnR =
        gson.fromJson(gson.toJson(txn), DepositTransactionResponse.class);

    setSharedTransactionResponseFields(txnR, txn);

    txnR.setDepositMemo(txn.getMemo());
    txnR.setDepositMemoType(txn.getMemoType());

    txnR.setMoreInfoUrl(moreInfoUrlConstructor.construct(txn));

    return txnR;
  }

  public WithdrawTransactionResponse fromWithdrawTxn(Sep24Transaction txn)
      throws MalformedURLException, URISyntaxException {

    WithdrawTransactionResponse txnR =
        gson.fromJson(gson.toJson(txn), WithdrawTransactionResponse.class);

    setSharedTransactionResponseFields(txnR, txn);

    txnR.setWithdrawMemo(txn.getMemo());
    txnR.setWithdrawMemoType(txn.getMemoType());
    txnR.setWithdrawAnchorAccount(txn.getWithdrawAnchorAccount());

    txnR.setMoreInfoUrl(moreInfoUrlConstructor.construct(txn));

    return txnR;
  }
}

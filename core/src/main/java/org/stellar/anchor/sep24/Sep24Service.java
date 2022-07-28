package org.stellar.anchor.sep24;

import static org.stellar.anchor.api.sep.SepTransactionStatus.INCOMPLETE;
import static org.stellar.anchor.sep24.Sep24Transaction.Kind.DEPOSIT;
import static org.stellar.anchor.sep24.Sep24Transaction.Kind.WITHDRAWAL;
import static org.stellar.anchor.sep9.Sep9Fields.extractSep9Fields;
import static org.stellar.anchor.util.Log.*;
import static org.stellar.anchor.util.MathHelper.decimal;
import static org.stellar.anchor.util.MemoHelper.makeMemo;
import static org.stellar.anchor.util.MemoHelper.memoType;
import static org.stellar.anchor.util.SepHelper.generateSepTransactionId;
import static org.stellar.anchor.util.SepHelper.memoTypeString;
import static org.stellar.anchor.util.SepLanguageHelper.validateLanguage;

import com.google.gson.Gson;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.*;
import org.apache.http.client.utils.URIBuilder;
import org.stellar.anchor.api.exception.SepException;
import org.stellar.anchor.api.exception.SepNotAuthorizedException;
import org.stellar.anchor.api.exception.SepNotFoundException;
import org.stellar.anchor.api.exception.SepValidationException;
import org.stellar.anchor.api.sep.AssetInfo;
import org.stellar.anchor.api.sep.sep24.*;
import org.stellar.anchor.asset.AssetService;
import org.stellar.anchor.auth.JwtService;
import org.stellar.anchor.auth.JwtToken;
import org.stellar.anchor.config.AppConfig;
import org.stellar.anchor.config.Sep24Config;
import org.stellar.sdk.KeyPair;
import org.stellar.sdk.Memo;

public class Sep24Service {
  final Gson gson;
  final AppConfig appConfig;
  final Sep24Config sep24Config;
  final AssetService assetService;
  final JwtService jwtService;
  final Sep24TransactionStore txnStore;

  public Sep24Service(
      Gson gson,
      AppConfig appConfig,
      Sep24Config sep24Config,
      AssetService assetService,
      JwtService jwtService,
      Sep24TransactionStore txnStore) {
    debug("appConfig:", appConfig);
    debug("sep24Config:", sep24Config);
    this.gson = gson;
    this.appConfig = appConfig;
    this.sep24Config = sep24Config;
    this.assetService = assetService;
    this.jwtService = jwtService;
    this.txnStore = txnStore;
    info("Sep24Service initialized.");
  }

  public InteractiveTransactionResponse withdraw(
      String fullRequestUrl, JwtToken token, Map<String, String> withdrawRequest)
      throws SepException, MalformedURLException, URISyntaxException {
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
    HashMap<String, String> sep9Fields = extractSep9Fields(withdrawRequest);

    String lang = validateLanguage(appConfig, withdrawRequest.get("lang"));
    debug("language: {}", lang);

    if (assetCode == null) {
      info("missing 'asset_code'");
      throw new SepValidationException("missing 'asset_code'");
    }

    if (sourceAccount == null) {
      info("missing 'account' field");
      throw new SepValidationException("'account' is required");
    }

    // Verify that the asset code exists in our database, with withdraw enabled.
    AssetInfo asset = assetService.getAsset(assetCode, assetIssuer);
    if (asset == null || !asset.getWithdraw().getEnabled() || !asset.getSep24Enabled()) {
      infoF("invalid operation for asset {}", assetCode);
      throw new SepValidationException(String.format("invalid operation for asset %s", assetCode));
    }

    // Validate amount
    if (strAmount != null) {
      if (decimal(strAmount).compareTo(decimal(asset.getWithdraw().getMinAmount())) < 0
          || decimal(strAmount).compareTo(decimal(asset.getWithdraw().getMaxAmount())) > 0) {
        infoF("invalid amount {}", strAmount);
        throw new SepValidationException(String.format("invalid amount: %s", strAmount));
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

    Memo memo = makeMemo(withdrawRequest.get("memo"), withdrawRequest.get("memo_type"));
    String txnId = UUID.randomUUID().toString();
    Sep24TransactionBuilder builder =
        new Sep24TransactionBuilder(txnStore)
            .transactionId(txnId)
            .status(INCOMPLETE.toString())
            .kind(WITHDRAWAL.toString())
            .amountIn(strAmount)
            .amountOut(strAmount)
            .assetCode(assetCode)
            .assetIssuer(withdrawRequest.get("asset_issuer"))
            .startedAt(Instant.now().getEpochSecond())
            .sep10Account(token.getAccount())
            .sep10AccountMemo(token.getAccountMemo())
            .fromAccount(sourceAccount)
            .clientDomain(token.getClientDomain());

    if (memo != null) {
      debug("transaction memo detected.", memo);
      builder.memo(memo.toString());
      builder.memoType(memoTypeString(memoType(memo)));
    }

    Sep24Transaction txn = builder.build();

    txnStore.save(txn);

    infoF(
        "Saved withdraw transaction. from={}, amountIn={}, amountOut={}.",
        shorter(txn.getFromAccount()),
        txn.getAmountIn(),
        txn.getAmountOut());
    debug("Transaction details:", txn);
    return new InteractiveTransactionResponse(
        "interactive_customer_info_needed",
        constructInteractiveUrl(
            "withdraw",
            buildRedirectJwtToken(fullRequestUrl, token, txn),
            sep9Fields,
            lang,
            assetCode,
            strAmount,
            txn.getTransactionId()),
        txn.getTransactionId());
  }

  public InteractiveTransactionResponse deposit(
      String fullRequestUrl, JwtToken token, Map<String, String> depositRequest)
      throws SepException, MalformedURLException, URISyntaxException {
    info("Creating withdrawal transaction.");
    if (token == null) {
      info("missing SEP-10 token");
      throw new SepValidationException("missing token");
    }

    if (depositRequest == null) {
      info("missing withdraw request");
      throw new SepValidationException("no request");
    }

    infoF(
        "Sep24.deposit. account={}, memo={}", shorter(token.getAccount()), token.getAccountMemo());
    String assetCode = depositRequest.get("asset_code");
    String assetIssuer = depositRequest.get("asset_issuer");
    String destinationAccount = depositRequest.get("account");
    String strAmount = depositRequest.get("amount");
    HashMap<String, String> sep9Fields = extractSep9Fields(depositRequest);

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

    if (destinationAccount == null) {
      throw new SepValidationException("'account' is required");
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

    // Validate amount
    if (strAmount != null) {
      if (decimal(strAmount).compareTo(decimal(asset.getDeposit().getMinAmount())) < 0
          || decimal(strAmount).compareTo(decimal(asset.getDeposit().getMaxAmount())) > 0) {
        infoF("invalid amount {}", strAmount);
        throw new SepValidationException(String.format("invalid amount: %s", strAmount));
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

    Memo memo = makeMemo(depositRequest.get("memo"), depositRequest.get("memo_type"));
    String txnId = generateSepTransactionId();
    Sep24TransactionBuilder builder =
        new Sep24TransactionBuilder(txnStore)
            .transactionId(txnId)
            .status(INCOMPLETE.toString())
            .kind(DEPOSIT.toString())
            .amountIn(strAmount)
            .amountOut(strAmount)
            .assetCode(assetCode)
            .assetIssuer(depositRequest.get("asset_issuer"))
            .startedAt(Instant.now().getEpochSecond())
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
    infoF(
        "Saved deposit transaction. to={}, amountIn={}, amountOut={}.",
        shorter(txn.getToAccount()),
        txn.getAmountIn(),
        txn.getAmountOut());
    debug("Transaction details:", txn);

    return new InteractiveTransactionResponse(
        "interactive_customer_info_needed",
        constructInteractiveUrl(
            "deposit",
            buildRedirectJwtToken(fullRequestUrl, token, txn),
            sep9Fields,
            lang,
            assetCode,
            strAmount,
            txn.getTransactionId()),
        txn.getTransactionId());
  }

  public GetTransactionsResponse findTransactions(JwtToken token, GetTransactionsRequest txReq)
      throws SepException, MalformedURLException, URISyntaxException {
    if (token == null) {
      info("missing SEP-10 token");
      throw new SepNotAuthorizedException("missing token");
    }

    infoF("findTransactions. account={}", shorter(token.getAccount()));
    if (assetService.getAsset(txReq.getAssetCode()) == null) {
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

  public GetTransactionResponse findTransaction(JwtToken token, GetTransactionRequest txReq)
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

    // If the token has a memo, make sure the transaction belongs to the account with the same memo.
    if (token.getAccountMemo() != null
        && !token.getAccountMemo().equals(txn.getSep10AccountMemo())) {
      infoF(
          "no transactions found with account:{} memo:{}",
          token.getAccount(),
          token.getAccountMemo());
      throw new SepNotFoundException("transaction not found");
    }

    return GetTransactionResponse.of(fromTxn(txn, txReq.getLang()));
  }

  public InfoResponse getInfo() {
    info("Getting Sep24 info");
    List<AssetInfo> assets = listAllAssets();
    InfoResponse info = new InfoResponse();
    info.setDeposit(new HashMap<>());
    info.setWithdraw(new HashMap<>());
    info.setFee(new InfoResponse.FeeResponse());
    info.setFeatureFlags(new InfoResponse.FeatureFlagResponse());

    debugF("{} assets found", assets.size());
    for (AssetInfo asset : assets) {
      if (asset.getDeposit().getEnabled())
        info.getDeposit().put(asset.getCode(), asset.getDeposit());
      if (asset.getWithdraw().getEnabled())
        info.getWithdraw().put(asset.getCode(), asset.getWithdraw());
    }
    return info;
  }

  TransactionResponse fromTxn(Sep24Transaction txn, String lang)
      throws MalformedURLException, URISyntaxException, SepException {
    debugF(
        "Converting Sep24Transaction to Transaction Response. kind={}, transactionId={}, lang={}",
        txn.getTransactionId(),
        txn.getTransactionId(),
        lang);
    TransactionResponse response;
    if (txn.getKind().equals(DEPOSIT.toString())) {
      response = Sep24Helper.fromDepositTxn(jwtService, sep24Config, txn, lang, true);
    } else if (txn.getKind().equals(WITHDRAWAL.toString())) {
      response = Sep24Helper.fromWithdrawTxn(jwtService, sep24Config, txn, lang, true);
    } else {
      throw new SepException(String.format("unsupported txn kind:%s", txn.getKind()));
    }

    // Calculate refund information.
    AssetInfo assetInfo = assetService.getAsset(txn.getAssetCode(), txn.getAssetIssuer());
    return Sep24Helper.updateRefundInfo(response, txn, assetInfo);
  }

  JwtToken buildRedirectJwtToken(String fullRequestUrl, JwtToken token, Sep24Transaction txn) {
    return JwtToken.of(
        fullRequestUrl,
        token.getSub(),
        Instant.now().getEpochSecond(),
        Instant.now().getEpochSecond() + sep24Config.getInteractiveJwtExpiration(),
        txn.getTransactionId(),
        token.getClientDomain());
  }

  List<AssetInfo> listAllAssets() {
    return this.assetService.listAllAssets();
  }

  String constructInteractiveUrl(
      String op,
      JwtToken token,
      HashMap<String, String> sep9Fields,
      String lang,
      String assetCode,
      String amount,
      String txnId)
      throws URISyntaxException, MalformedURLException {

    String interactiveUrlHostname = sep24Config.getInteractiveUrl();

    URI uri = new URI(interactiveUrlHostname);

    URIBuilder builder =
        new URIBuilder()
            .setScheme(uri.getScheme())
            .setHost(uri.getHost())
            .setPort(uri.getPort())
            .setPath(uri.getPath())
            .addParameter("operation", op)
            .addParameter("asset_code", assetCode)
            .addParameter("transaction_id", txnId)
            .addParameter("token", jwtService.encode(token));

    if (amount != null) {
      builder.addParameter("amount", amount);
    }

    if (lang != null) {
      builder.addParameter("lang", lang);
    }

    // Add Sep9 fields to url
    sep9Fields.forEach(builder::addParameter);

    return builder.build().toURL().toString();
  }
}

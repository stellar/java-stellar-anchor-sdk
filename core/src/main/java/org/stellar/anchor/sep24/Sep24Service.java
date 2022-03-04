package org.stellar.anchor.sep24;

import static org.stellar.anchor.model.Sep24Transaction.Kind.DEPOSIT;
import static org.stellar.anchor.model.Sep24Transaction.Kind.WITHDRAWAL;
import static org.stellar.anchor.util.Log.shorter;
import static org.stellar.anchor.util.SepUtil.memoTypeString;
import static org.stellar.sdk.xdr.MemoType.MEMO_ID;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.*;
import org.apache.commons.codec.binary.Hex;
import org.apache.http.client.utils.URIBuilder;
import org.stellar.anchor.asset.AssetResponse;
import org.stellar.anchor.asset.AssetService;
import org.stellar.anchor.config.AppConfig;
import org.stellar.anchor.config.Sep24Config;
import org.stellar.anchor.dto.sep24.*;
import org.stellar.anchor.exception.SepException;
import org.stellar.anchor.exception.SepNotFoundException;
import org.stellar.anchor.exception.SepValidationException;
import org.stellar.anchor.model.Sep24Transaction;
import org.stellar.anchor.model.Sep24TransactionBuilder;
import org.stellar.anchor.sep10.JwtService;
import org.stellar.anchor.sep10.JwtToken;
import org.stellar.anchor.sep9.Sep9Fields;
import org.stellar.anchor.util.Log;
import org.stellar.sdk.*;
import org.stellar.sdk.xdr.MemoType;

public class Sep24Service {
  final AppConfig appConfig;
  final Sep24Config sep24Config;
  final AssetService assetService;
  final JwtService jwtService;
  final Sep24TransactionStore txnStore;

  public Sep24Service(
      AppConfig appConfig,
      Sep24Config sep24Config,
      AssetService assetService,
      JwtService jwtService,
      Sep24TransactionStore txnStore) {
    this.appConfig = appConfig;
    this.sep24Config = sep24Config;
    this.assetService = assetService;
    this.jwtService = jwtService;
    this.txnStore = txnStore;
    Log.info("Initializing sep24 service.");
  }

  public InteractiveTransactionResponse withdraw(
      String fullRequestUrl, JwtToken token, Map<String, String> withdrawRequest)
      throws SepException, MalformedURLException, URISyntaxException {
    if (token == null) {
      throw new SepValidationException("missing token");
    }

    if (withdrawRequest == null) {
      throw new SepValidationException("no request");
    }

    Log.infoF(
        "Sep24.withdraw. account={}, memo={}", shorter(token.getAccount()), token.getAccountMemo());
    String lang = withdrawRequest.get("lang");
    String assetCode = withdrawRequest.get("asset_code");
    String assetIssuer = withdrawRequest.get("asset_issuer");
    String sourceAccount = withdrawRequest.get("account");
    String strAmount = withdrawRequest.get("amount");
    HashMap<String, String> sep9Fields = extractSep9Fields(withdrawRequest);

    validateAndActivateLanguage(lang);

    if (assetCode == null) {
      throw new SepValidationException("missing 'asset_code'");
    }

    if (sourceAccount == null) {
      throw new SepValidationException("'account' is required");
    }

    if (!sourceAccount.equals(token.getAccount())) {
      throw new SepValidationException("'account' does not match the one in the token");
    }

    // Verify that the asset code exists in our database, with withdraw enabled.
    AssetResponse asset = assetService.getAsset(assetCode, assetIssuer);
    if (asset == null || !asset.getWithdraw().getEnabled() || !asset.getSep24Enabled()) {
      throw new SepValidationException(String.format("invalid operation for asset %s", assetCode));
    }

    // Validate amount
    if (strAmount != null) {
      if (new BigDecimal(strAmount).compareTo(new BigDecimal(asset.getWithdraw().getMinAmount()))
              < 0
          || new BigDecimal(strAmount).compareTo(new BigDecimal(asset.getWithdraw().getMaxAmount()))
              > 0) {
        throw new SepValidationException(String.format("invalid amount: %s", strAmount));
      }
    }

    // Validate sourceAccount
    try {
      KeyPair.fromAccountId(sourceAccount);
    } catch (Exception ex) {
      throw new SepValidationException(String.format("invalid account: %s", sourceAccount), ex);
    }

    String txnId = UUID.randomUUID().toString();
    Sep24Transaction txn =
        new Sep24TransactionBuilder(txnStore)
            .transactionId(txnId)
            .status(Sep24Transaction.Status.INCOMPLETE.toString())
            .kind(Sep24Transaction.Kind.WITHDRAWAL.toString())
            .amountIn(strAmount)
            .amountOut(strAmount)
            .assetCode(assetCode)
            .assetIssuer(withdrawRequest.get("asset_issuer"))
            .startedAt(Instant.now().getEpochSecond())
            .stellarAccount(token.getAccount())
            .fromAccount(sourceAccount)
            .protocol(Sep24Transaction.Protocol.SEP24.toString())
            .memoType(memoTypeString(MEMO_ID))
            .domainClient(token.getClientDomain())
            .build();

    txnStore.save(txn);

    Log.infoF(
        "Saved withdraw transaction. from={}, amountIn={}, amountOut={}.",
        shorter(txn.getFromAccount()),
        txn.getAmountIn(),
        txn.getAmountOut());
    Log.debug("Transaction details:", txn);
    return new InteractiveTransactionResponse(
        "interactive_customer_info_needed",
        constructInteractiveUrl(
            "withdraw",
            buildRedirectJwtToken(fullRequestUrl, token, txn),
            sep9Fields,
            assetCode,
            strAmount,
            txn.getTransactionId()),
        txn.getTransactionId());
  }

  public InteractiveTransactionResponse deposit(
      String fullRequestUrl, JwtToken token, Map<String, String> depositRequest)
      throws SepException, MalformedURLException, URISyntaxException {
    if (token == null) {
      throw new SepValidationException("missing token");
    }

    if (depositRequest == null) {
      throw new SepValidationException("no request");
    }

    Log.infoF(
        "Sep24.deposit. account={}, memo={}", shorter(token.getAccount()), token.getAccountMemo());

    String lang = depositRequest.get("lang");
    String assetCode = depositRequest.get("asset_code");
    String assetIssuer = depositRequest.get("asset_issuer");
    String destinationAccount = depositRequest.get("account");
    String strAmount = depositRequest.get("amount");
    HashMap<String, String> sep9Fields = extractSep9Fields(depositRequest);

    String strClaimableSupported = depositRequest.get("claimable_balance_supported");
    boolean claimableSupported = false;
    if (strClaimableSupported != null) {
      claimableSupported = Boolean.parseBoolean(strClaimableSupported.toLowerCase(Locale.ROOT));
    }

    validateAndActivateLanguage(lang);

    if (assetCode == null) {
      throw new SepValidationException("missing 'asset_code'");
    }

    if (destinationAccount == null) {
      throw new SepValidationException("'account' is required");
    }

    if (!destinationAccount.equals(token.getAccount())) {
      throw new SepValidationException("'account' does not match the one in the token");
    }

    makeMemo(depositRequest.get("memo"), depositRequest.get("memo_type"));

    // Verify that the asset code exists in our database, with withdraw enabled.
    AssetResponse asset = assetService.getAsset(assetCode, assetIssuer);
    if (asset == null || !asset.getDeposit().getEnabled() || !asset.getSep24Enabled()) {
      throw new SepValidationException(String.format("invalid operation for asset %s", assetCode));
    }

    // Validate amount
    if (strAmount != null) {
      if (new BigDecimal(strAmount).compareTo(new BigDecimal(asset.getDeposit().getMinAmount())) < 0
          || new BigDecimal(strAmount).compareTo(new BigDecimal(asset.getDeposit().getMaxAmount()))
              > 0) {
        throw new SepValidationException(String.format("invalid amount: %s", strAmount));
      }
    }

    try {
      KeyPair.fromAccountId(destinationAccount);
    } catch (Exception ex) {
      throw new SepValidationException(
          String.format("invalid account: %s", destinationAccount), ex);
    }

    String txnId = UUID.randomUUID().toString();
    Sep24Transaction txn =
        new Sep24TransactionBuilder(txnStore)
            .transactionId(txnId)
            .status(Sep24Transaction.Status.INCOMPLETE.toString())
            .kind(Sep24Transaction.Kind.DEPOSIT.toString())
            .amountIn(strAmount)
            .amountOut(strAmount)
            .assetCode(assetCode)
            .assetIssuer(depositRequest.get("asset_issuer"))
            .startedAt(Instant.now().getEpochSecond())
            .stellarAccount(token.getAccount())
            .toAccount(destinationAccount)
            .protocol(Sep24Transaction.Protocol.SEP24.toString())
            .domainClient(token.getClientDomain())
            .claimableBalanceSupported(claimableSupported)
            .build();

    txnStore.save(txn);
    Log.infoF(
        "Saved deposit transaction. to={}, amountIn={}, amountOut={}.",
        shorter(txn.getToAccount()),
        txn.getAmountIn(),
        txn.getAmountOut());
    Log.debug("Transaction details:", txn);

    return new InteractiveTransactionResponse(
        "interactive_customer_info_needed",
        constructInteractiveUrl(
            "deposit",
            buildRedirectJwtToken(fullRequestUrl, token, txn),
            sep9Fields,
            assetCode,
            strAmount,
            txn.getTransactionId()),
        txn.getTransactionId());
  }

  public GetTransactionsResponse findTransactions(JwtToken token, GetTransactionsRequest txReq)
      throws SepException, MalformedURLException, URISyntaxException {
    if (token == null) {
      throw new SepValidationException("missing token");
    }

    Log.infoF("Sep24.findTransactions. account={}", shorter(token.getAccount()));
    if (assetService.getAsset(txReq.getAssetCode(), null) == null) {
      throw new SepValidationException("asset code is not supported");
    }
    List<Sep24Transaction> txns = txnStore.findTransactions(token.getAccount(), txReq);
    GetTransactionsResponse result = new GetTransactionsResponse();
    List<TransactionResponse> list = new ArrayList<>();
    for (Sep24Transaction txn : txns) {
      TransactionResponse transactionResponse = fromTxn(txn);
      list.add(transactionResponse);
    }
    result.setTransactions(list);

    return result;
  }

  public GetTransactionResponse findTransaction(JwtToken token, GetTransactionRequest txReq)
      throws SepException, IOException, URISyntaxException {
    if (token == null) {
      throw new SepValidationException("missing token");
    }

    if (txReq == null) {
      throw new SepValidationException("missing request object");
    }

    Log.infoF("Sep24.findTransaction. account={}", shorter(token.getAccount()));

    Sep24Transaction txn;
    if (txReq.getId() != null) {
      Log.infoF("id={}", txReq.getId());
      txn = txnStore.findByTransactionId(txReq.getId());
    } else if (txReq.getStellarTransactionId() != null) {
      Log.infoF("stellarTransactionId={}", shorter(txReq.getStellarTransactionId()));
      txn = txnStore.findByStellarTransactionId(txReq.getStellarTransactionId());
    } else if (txReq.getExternalTransactionId() != null) {
      Log.infoF("externalTransactionId={}", shorter(txReq.getExternalTransactionId()));
      txn = txnStore.findByExternalTransactionId(txReq.getExternalTransactionId());
    } else {
      throw new SepValidationException(
          "One of id, stellar_transaction_id or external_transaction_id is required.");
    }

    // We should not return the transaction that belongs to other accounts.
    if (txn == null || !txn.getStellarAccount().equals(token.getAccount())) {
      throw new SepNotFoundException("transaction is not found");
    }

    return GetTransactionResponse.of(fromTxn(txn));
  }

  public InfoResponse getInfo() {
    List<AssetResponse> assets = listAllAssets();
    InfoResponse info = new InfoResponse();
    info.setDeposit(new HashMap<>());
    info.setWithdraw(new HashMap<>());
    info.setFee(new InfoResponse.FeeResponse());
    info.setFeatureFlags(new InfoResponse.FeatureFlagResponse());

    for (AssetResponse asset : assets) {
      if (asset.getDeposit().getEnabled())
        info.getDeposit().put(asset.getCode(), asset.getDeposit());
      if (asset.getWithdraw().getEnabled())
        info.getWithdraw().put(asset.getCode(), asset.getWithdraw());
    }
    return info;
  }

  String convertBase64ToHex(String memo) {
    return Hex.encodeHexString(Base64.getDecoder().decode(memo.getBytes()));
  }

  TransactionResponse fromTxn(Sep24Transaction txn)
      throws MalformedURLException, URISyntaxException, SepException {
    if (txn.getKind().equals(DEPOSIT.toString())) {
      return DepositTransactionResponse.of(jwtService, sep24Config, txn, true);
    } else if (txn.getKind().equals(WITHDRAWAL.toString())) {
      return WithdrawTransactionResponse.of(jwtService, sep24Config, txn, true);
    } else {
      throw new SepException(String.format("unsupported txn kind:%s", txn.getKind()));
    }
  }

  JwtToken buildRedirectJwtToken(String fullRequestUrl, JwtToken token, Sep24Transaction txn) {
    return JwtToken.of(
        fullRequestUrl,
        token.getAccount(),
        Instant.now().getEpochSecond(),
        Instant.now().getEpochSecond() + sep24Config.getInteractiveJwtExpiration(),
        txn.getTransactionId(),
        token.getClientDomain());
  }

  List<AssetResponse> listAllAssets() {
    return this.assetService.listAllAssets();
  }

  @SuppressWarnings("UnusedReturnValue")
  Memo makeMemo(String memo, String memoType) throws SepException {
    if (memo == null || memoType == null) {
      return null;
    }
    MemoType mt;
    switch (memoType) {
      case "text":
        mt = MemoType.MEMO_TEXT;
        break;
      case "id":
        mt = MEMO_ID;
        break;
      case "hash":
        mt = MemoType.MEMO_HASH;
        break;
      case "none":
      case "return":
        throw new SepException("Unsupported value: " + memoType);
      default:
        throw new SepValidationException(String.format("Invalid memo type: %s", memoType));
    }

    return makeMemo(memo, mt);
  }

  Memo makeMemo(String memo, MemoType memoType) throws SepException {
    try {
      switch (memoType) {
        case MEMO_ID:
          return new MemoId(Long.parseLong(memo));
        case MEMO_TEXT:
          return new MemoText(memo);
        case MEMO_HASH:
          return new MemoHash(convertBase64ToHex(memo));
        default:
          throw new SepException("Unsupported value: " + memoType);
      }
    } catch (NumberFormatException nfex) {
      throw new SepValidationException(
          String.format("Invalid memo %s of type:%s", memo, memoType), nfex);
    }
  }

  String constructInteractiveUrl(
      String op,
      JwtToken token,
      HashMap<String, String> sep9Fields,
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
    // Add Sep9 fields to url
    sep9Fields.forEach(builder::addParameter);

    return builder.build().toURL().toString();
  }

  HashMap<String, String> extractSep9Fields(Map<String, String> wr) {
    HashMap<String, String> sep9Fields = new HashMap<>();
    for (Map.Entry<String, String> entry : wr.entrySet()) {
      if (Sep9Fields.SEP_9_FIELDS.contains(entry.getKey())) {
        sep9Fields.put(entry.getKey(), entry.getValue());
      }
    }
    return sep9Fields;
  }

  void validateAndActivateLanguage(String lang) throws SepValidationException {
    if (lang != null) {
      List<String> languages = appConfig.getLanguages();
      if (languages != null && languages.size() > 0) {
        if (languages.stream().noneMatch(l -> l.equalsIgnoreCase(lang))) {
          throw new SepValidationException(String.format("unsupported language: %s", lang));
        }
      }
      // TODO: Implement later
      // activateLanguage();
    }
  }
}

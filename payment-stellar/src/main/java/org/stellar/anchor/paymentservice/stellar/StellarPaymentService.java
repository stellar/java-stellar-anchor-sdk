package org.stellar.anchor.paymentservice.stellar;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.stellar.anchor.exception.HttpException;
import org.stellar.anchor.paymentservice.*;
import org.stellar.anchor.paymentservice.Account;
import org.stellar.anchor.paymentservice.Network;
import org.stellar.anchor.paymentservice.stellar.requests.SubmitTransactionRequest;
import org.stellar.anchor.paymentservice.stellar.util.NettyHttpClient;
import org.stellar.sdk.*;
import org.stellar.sdk.responses.AccountResponse;
import org.stellar.sdk.responses.Page;
import org.stellar.sdk.responses.SubmitTransactionResponse;
import org.stellar.sdk.responses.TransactionResponse;
import org.stellar.sdk.responses.operations.*;
import reactor.core.publisher.Mono;
import reactor.netty.ByteBufMono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.client.HttpClientResponse;
import reactor.util.annotation.Nullable;

public class StellarPaymentService implements PaymentService {
  private static final Gson gson = new Gson();
  private final Network network = Network.STELLAR;
  private String url;
  private String secretKey;
  private int baseFee;
  private org.stellar.sdk.Network stellarNetwork;
  long transactionsExpireAfter;

  private HttpClient webClient;

  public StellarPaymentService(
      String url,
      String secretKey,
      int baseFee,
      org.stellar.sdk.Network stellarNetwork,
      long transactionsExpireAfter) {
    this.url = url;
    this.secretKey = secretKey;
    this.baseFee = baseFee;
    this.stellarNetwork = stellarNetwork;
    this.transactionsExpireAfter = transactionsExpireAfter;
  }

  public Network getNetwork() {
    return network;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getSecretKey() {
    return secretKey;
  }

  public void setSecretKey(String secretKey) {
    this.secretKey = secretKey;
  }

  public int getBaseFee() {
    return baseFee;
  }

  public void setBaseFee(int baseFee) {
    this.baseFee = baseFee;
  }

  public org.stellar.sdk.Network getStellarNetwork() {
    return stellarNetwork;
  }

  public void setStellarNetwork(org.stellar.sdk.Network stellarNetwork) {
    this.stellarNetwork = stellarNetwork;
  }

  public long getTransactionsExpireAfter() {
    return transactionsExpireAfter;
  }

  public void setTransactionsExpireAfter(long transactionsExpireAfter) {
    this.transactionsExpireAfter = transactionsExpireAfter;
  }

  private HttpClient getWebClient() {
    if (webClient == null) {
      webClient = NettyHttpClient.withBaseUrl(getUrl());
    }
    return webClient;
  }

  public Mono<Void> ping() throws HttpException {
    return getWebClient()
        .get()
        .response()
        .flatMap(
            response -> {
              if (response.status().code() != 200) {
                return Mono.error(new HttpException(response.status().code()));
              }
              return Mono.empty();
            });
  }

  public Mono<Void> validateSecretKey() throws HttpException {
    return getDistributionAccountAddress()
        .flatMap(this::getAccount)
        .flatMap(account -> Mono.empty());
  }

  public Mono<String> getDistributionAccountAddress() throws HttpException {
    if (secretKey == null) {
      return null;
    }
    return Mono.just(KeyPair.fromSecretSeed(secretKey).getAccountId());
  }

  public Mono<Account> getAccount(String accountId) throws HttpException {
    return getAccountResponse(accountId)
        .flatMap(accountResponse -> Mono.just(accountResponseToAccount(accountResponse)));
  }

  public Mono<AccountResponse> getAccountResponse(String accountId) throws HttpException {
    return getWebClient()
        .get()
        .uri("/accounts/" + accountId)
        .responseSingle(
            (response, bodyBytesMono) -> {
              if (response.status().code() != 200) {
                return handleStellarHttpError(response, "unable to fetch account " + accountId);
              }
              return bodyBytesMono.asString();
            })
        .flatMap(body -> Mono.just(gson.fromJson(body, AccountResponse.class)));
  }

  private Account accountResponseToAccount(AccountResponse accountResponse) {
    Account account =
        new Account(
            network, accountResponse.getAccountId(), new Account.Capabilities(Network.CIRCLE));
    List<Balance> balances = new ArrayList<Balance>();
    for (AccountResponse.Balance b : accountResponse.getBalances()) {
      if (!b.getAsset().isPresent() || !b.getAuthorized()) {
        // ignored liquidity pool shares or frozen balances
        continue;
      }
      balances.add(
          new Balance(b.getBalance(), "stellar:" + b.getAssetCode() + ":" + b.getAssetIssuer()));
    }
    account.setBalances(balances);
    return account;
  }

  public Mono<Account> createAccount(String accountId) throws HttpException {
    return createAccount(accountId, new BigDecimal("1"));
  }

  public Mono<Account> createAccount(String accountId, BigDecimal withAmount) throws HttpException {
    return getAccountResponse(KeyPair.fromSecretSeed(secretKey).getAccountId())
        .flatMap(
            accountResponse ->
                Mono.just(
                    buildCreateAccountTransactionEnvelope(accountResponse, accountId, withAmount)))
        .flatMap(this::submitTransaction)
        .flatMap(transactionResponse -> getAccount(accountId));
  }

  private Mono<SubmitTransactionResponse> submitTransaction(String envelope) {
    String requestBody = gson.toJson(new SubmitTransactionRequest(envelope));
    return getWebClient()
        .post()
        .uri("/transactions")
        .send(ByteBufMono.fromString(Mono.just(requestBody)))
        .responseSingle(
            (postResponse, bodyBytesMono) -> {
              if (postResponse.status().code() != 200) {
                return handleStellarHttpError(
                    postResponse,
                    "non-success response returned when submitting a transaction, envelope: "
                        + envelope.toString());
              }
              return bodyBytesMono.asString();
            })
        .flatMap(
            responseBody ->
                Mono.just(gson.fromJson(responseBody, SubmitTransactionResponse.class)));
  }

  private String buildCreateAccountTransactionEnvelope(
      AccountResponse accountResponse, String accountId, BigDecimal withAmount) {
    CreateAccountOperation createAccountOp =
        new CreateAccountOperation.Builder(accountId, withAmount.toString()).build();
    Transaction transaction =
        new Transaction.Builder(accountResponse, stellarNetwork)
            .addOperation(createAccountOp)
            .addMemo(Memo.none())
            .setTimeout(transactionsExpireAfter)
            .setBaseFee(baseFee)
            .build();
    return transaction.toEnvelopeXdr().toString();
  }

  public Mono<PaymentHistory> getAccountPaymentHistory(String accountID, @Nullable String cursor)
      throws HttpException {
    String params = "?order=desc&join=transactions";
    if (cursor != null) {
      params += "&cursor=" + cursor;
    }
    return getWebClient()
        .get()
        .uri("/payments" + params)
        .responseSingle(
            (response, bodyBufMono) -> {
              if (response.status().code() != 200) {
                return handleStellarHttpError(
                    response, "unable to fetch payment history with uri: " + response.uri());
              }
              return bodyBufMono.asString();
            })
        .flatMap(
            body -> {
              Page<OperationResponse> operationsPage =
                  gson.fromJson(body, new TypeToken<Page<OperationResponse>>() {}.getType());
              return Mono.just(makePaymentHistoryFrom(operationsPage, accountID));
            });
  }

  private PaymentHistory makePaymentHistoryFrom(
      Page<OperationResponse> operationsPage, String forAccountId) {
    // does not fetch account balances for each source and destination account
    // denominates path payment amounts in units of the asset that is known prior to submission
    // TODO: update Payment to account for different send and receive balances
    Account forAccount =
        new Account(network, forAccountId, new Account.Capabilities(Network.CIRCLE));
    PaymentHistory paymentHistory = new PaymentHistory();
    paymentHistory.setAccount(forAccount);
    List<OperationResponse> opResps = operationsPage.getRecords();
    if (opResps.size() == 0) {
      return paymentHistory;
    }
    paymentHistory.setCursor(opResps.get(opResps.size() - 1).getPagingToken());
    paymentHistory.setPayments(
        opResps.stream().map(this::createPaymentFromStellarOperation).collect(Collectors.toList()));
    return paymentHistory;
  }

  private Payment createPaymentFromStellarOperation(OperationResponse opResp) {
    String sourceAccount, destinationAccount, currencyName, amount;
    if (opResp.getType().equals("create_account")) {
      CreateAccountOperationResponse createAccountOpResp = (CreateAccountOperationResponse) opResp;
      sourceAccount = createAccountOpResp.getFunder();
      destinationAccount = createAccountOpResp.getAccount();
      currencyName = "stellar:native";
      amount = createAccountOpResp.getStartingBalance();
    } else if (opResp.getType().equals("payment")) {
      PaymentOperationResponse paymentOpResp = (PaymentOperationResponse) opResp;
      sourceAccount = paymentOpResp.getFrom();
      destinationAccount = paymentOpResp.getTo();
      currencyName = getAssetIdentifier(paymentOpResp.getAsset());
      amount = paymentOpResp.getAmount();
    } else if (opResp.getType().equals("path_payment_strict_send")) {
      PathPaymentStrictSendOperationResponse paymentStrictSendOpResp =
          (PathPaymentStrictSendOperationResponse) opResp;
      sourceAccount = paymentStrictSendOpResp.getFrom();
      destinationAccount = paymentStrictSendOpResp.getTo();
      currencyName = getAssetIdentifier(paymentStrictSendOpResp.getSourceAsset());
      amount = paymentStrictSendOpResp.getSourceAmount();
    } else if (opResp.getType().equals("path_payment_strict_receive")) {
      PathPaymentStrictReceiveOperationResponse paymentStrictReceiveOpResp =
          (PathPaymentStrictReceiveOperationResponse) opResp;
      sourceAccount = paymentStrictReceiveOpResp.getFrom();
      destinationAccount = paymentStrictReceiveOpResp.getTo();
      currencyName = getAssetIdentifier(paymentStrictReceiveOpResp.getAsset());
      amount = paymentStrictReceiveOpResp.getAmount();
    } else if (opResp.getType().equals("account_merge")) {
      AccountMergeOperationResponse accountMergeOpResp = (AccountMergeOperationResponse) opResp;
      sourceAccount = accountMergeOpResp.getAccount();
      destinationAccount = accountMergeOpResp.getInto();
      currencyName = "stellar:native";
      // TODO: join w/ transactions, fetch result XDR, decode amount merged to destination
      amount = null;
    } else {
      throw new RuntimeException("unexpected payment operation: " + opResp.getType());
    }
    return createPayment(
        opResp.getTransactionHash(),
        new Account(network, sourceAccount, new Account.Capabilities(Network.CIRCLE)),
        new Account(network, destinationAccount, new Account.Capabilities(Network.CIRCLE)),
        currencyName,
        amount,
        opResp.getCreatedAt());
  }

  private String getAssetIdentifier(Asset asset) {
    if (asset.getType().equals("native")) {
      return "stellar:native";
    } else if (asset.getType().contains("alphanum")) {
      AssetTypeCreditAlphaNum alphaAsset = (AssetTypeCreditAlphaNum) asset;
      return "stellar:" + alphaAsset.getCode() + ":" + alphaAsset.getIssuer();
    } else {
      throw new RuntimeException("unexpected asset type: " + asset.getType());
    }
  }

  public Mono<Payment> sendPayment(
      Account sourceAccount, Account destinationAccount, String currencyName, BigDecimal amount)
      throws HttpException {
    return sendPayment(sourceAccount, destinationAccount, currencyName, amount, null, null);
  }

  public Mono<Payment> sendPayment(
      Account sourceAccount,
      Account destinationAccount,
      String currencyName,
      BigDecimal amount,
      @Nullable String memo,
      @Nullable String memoType)
      throws HttpException {
    return getAccountResponse(sourceAccount.id)
        .flatMap(
            accountResponse ->
                Mono.just(
                    buildPaymentTransactionEnvelope(
                        accountResponse,
                        destinationAccount.id,
                        currencyName,
                        amount,
                        memo,
                        memoType)))
        .flatMap(this::submitTransaction)
        .flatMap(
            submitTransactionResponse -> getStellarTransaction(submitTransactionResponse.getHash()))
        .flatMap(
            transactionResponse ->
                Mono.just(
                    createPayment(
                        transactionResponse.getHash(),
                        sourceAccount,
                        destinationAccount,
                        currencyName,
                        amount.toString(),
                        transactionResponse.getCreatedAt())));
  }

  private Payment createPayment(
      String hash,
      Account sourceAccount,
      Account destinationAccount,
      String currencyName,
      String amount,
      String createdAt) {
    Payment payment = new Payment();
    payment.setId(hash);
    payment.setSourceAccount(sourceAccount);
    payment.setDestinationAccount(destinationAccount);
    payment.setBalance(new Balance(amount, currencyName));
    payment.setStatus(Payment.Status.SUCCESSFUL);
    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    try {
      payment.setCreatedAt(formatter.parse(createdAt));
      payment.setUpdatedAt(formatter.parse(createdAt));
    } catch (ParseException e) {
      throw new RuntimeException("unable to parse datetime string from Horizon: " + createdAt);
    }
    return payment;
  }

  private String buildPaymentTransactionEnvelope(
      AccountResponse accountResponse,
      String accountId,
      String currencyName,
      BigDecimal amount,
      @Nullable String memo,
      @Nullable String memoType) {
    Asset asset;
    if (Arrays.asList("xlm", "native").contains(currencyName.toLowerCase())) {
      asset = new AssetTypeNative();
    } else if (currencyName.length() > 4) {
      asset = new AssetTypeCreditAlphaNum12(currencyName, amount.toString());
    } else {
      asset = new AssetTypeCreditAlphaNum4(currencyName, amount.toString());
    }
    PaymentOperation paymentOp =
        new PaymentOperation.Builder(accountId, asset, amount.toString()).build();
    Transaction.Builder builder =
        new Transaction.Builder(accountResponse, stellarNetwork)
            .addOperation(paymentOp)
            .setTimeout(transactionsExpireAfter)
            .setBaseFee(baseFee);
    Memo memoObj = makeMemo(memo, memoType);
    if (memoObj != null) {
      builder.addMemo(memoObj);
    }
    Transaction transaction = builder.build();
    return transaction.toEnvelopeXdr().toString();
  }

  private Memo makeMemo(@Nullable String memo, @Nullable String memoType) {
    if (memo == null && memoType == null) {
      return null;
    } else if (memo == null || memoType == null) {
      throw new IllegalArgumentException("both or neither 'memo' and 'memoType' must be passed");
    } else if (!Arrays.asList("hash", "id", "text").contains(memoType)) {
      throw new IllegalArgumentException("'memoType', must be one of 'hash', 'id', or 'text'");
    } else if (memoType.equals("hash")) {
      return new MemoHash(memo);
    } else if (memoType.equals("text")) {
      return new MemoText(memo);
    } else {
      return new MemoId(Long.parseLong(memo));
    }
  }

  public Mono<TransactionResponse> getStellarTransaction(String hash) {
    return getWebClient()
        .get()
        .uri("/transactions/" + hash)
        .responseSingle(
            (response, bodyBytesMono) -> {
              if (response.status().code() != 200) {
                return handleStellarHttpError(response, "unable to fetch transaction " + hash);
              }
              return bodyBytesMono.asString();
            })
        .flatMap(body -> Mono.just(gson.fromJson(body, TransactionResponse.class)));
  }

  public Mono<DepositInstructions> getDepositInstructions(DepositRequirements config)
      throws HttpException {
    return null;
  }

  private static <T> Mono<T> handleStellarHttpError(
      HttpClientResponse response, @Nullable String reason) throws HttpException {
    return Mono.error(new HttpException(response.status().code(), reason));
  }
}

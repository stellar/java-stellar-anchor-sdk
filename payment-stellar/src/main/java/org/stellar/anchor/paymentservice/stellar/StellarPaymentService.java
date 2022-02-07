package org.stellar.anchor.paymentservice.stellar;

import com.google.gson.Gson;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import org.stellar.anchor.exception.HttpException;
import org.stellar.anchor.paymentservice.*;
import org.stellar.anchor.paymentservice.Account;
import org.stellar.anchor.paymentservice.Network;
import org.stellar.anchor.paymentservice.stellar.requests.SubmitTransactionRequest;
import org.stellar.anchor.paymentservice.stellar.util.NettyHttpClient;
import org.stellar.sdk.*;
import org.stellar.sdk.responses.AccountResponse;
import org.stellar.sdk.responses.SubmitTransactionResponse;
import org.stellar.sdk.responses.TransactionResponse;
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
    return null;
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

  public Mono<PaymentHistory> getAccountPaymentHistory(
      String accountID, @Nullable String beforeCursor, @Nullable String afterCursor)
      throws HttpException {
    return null;
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
                    getPaymentFromTransactionResponse(
                        transactionResponse,
                        sourceAccount,
                        destinationAccount,
                        currencyName,
                        amount)));
  }

  private Payment getPaymentFromTransactionResponse(
      TransactionResponse response,
      Account sourceAccount,
      Account destinationAccount,
      String currencyName,
      BigDecimal amount) {
    Payment payment = new Payment();
    payment.setId(response.getHash());
    payment.setSourceAccount(sourceAccount);
    payment.setDestinationAccount(destinationAccount);
    payment.setBalance(new Balance(amount.toString(), currencyName));
    payment.setStatus(Payment.Status.SUCCESSFUL);
    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    try {
      payment.setCreatedAt(formatter.parse(response.getCreatedAt()));
      payment.setUpdatedAt(formatter.parse(response.getCreatedAt()));
    } catch (ParseException e) {
      throw new RuntimeException(
          "unable to parse datetime string from Horizon: " + response.getCreatedAt());
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

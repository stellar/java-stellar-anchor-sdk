package org.stellar.anchor.paymentservice.stellar;

import com.google.gson.Gson;
import java.math.BigDecimal;
import org.stellar.anchor.exception.HttpException;
import org.stellar.anchor.paymentservice.Account;
import org.stellar.anchor.paymentservice.DepositInstructions;
import org.stellar.anchor.paymentservice.DepositRequirements;
import org.stellar.anchor.paymentservice.Network;
import org.stellar.anchor.paymentservice.Payment;
import org.stellar.anchor.paymentservice.PaymentHistory;
import org.stellar.anchor.paymentservice.PaymentService;
import org.stellar.anchor.paymentservice.stellar.requests.SubmitTransactionRequest;
import org.stellar.anchor.paymentservice.stellar.util.NettyHttpClient;
import org.stellar.sdk.*;
import org.stellar.sdk.responses.AccountResponse;
import org.stellar.sdk.responses.SubmitTransactionResponse;
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

  public StellarPaymentService(String url, String secretKey, int baseFee, org.stellar.sdk.Network stellarNetwork, long transactionsExpireAfter) {
    super();
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

  public reactor.core.publisher.Mono<Void> ping() throws HttpException {
    return null;
  }

  public reactor.core.publisher.Mono<Void> validateSecretKey() throws HttpException {
    return getDistributionAccountAddress()
        .flatMap(this::getAccount)
        .flatMap(account -> Mono.empty());
  }

  public reactor.core.publisher.Mono<String> getDistributionAccountAddress() throws HttpException {
    if (secretKey == null) {
      return null;
    }
    return Mono.just(KeyPair.fromSecretSeed(secretKey).getAccountId());
  }

  public reactor.core.publisher.Mono<Account> getAccount(String accountId) throws HttpException {
    return getAccountResponse(accountId)
        .flatMap(accountResponse -> Mono.just(accountResponseToAccount(accountResponse)));
  }

  public reactor.core.publisher.Mono<AccountResponse> getAccountResponse(String accountId)
      throws HttpException {
    return getWebClient()
        .get()
        .uri("/accounts/" + accountId)
        .responseSingle(
            (response, bodyBytesMono) -> {
              if (response.status().code() >= 400) {
                return handleStellarGetError(response, bodyBytesMono);
              }
              return bodyBytesMono.asString();
            })
        .flatMap(body -> Mono.just(gson.fromJson(body, AccountResponse.class)));
  }

  private Account accountResponseToAccount(AccountResponse accountResponse) {
    return null;
  }

  public reactor.core.publisher.Mono<Account> createAccount(String accountId) throws HttpException {
    return createAccount(accountId, new BigDecimal("1"));
  }

  public reactor.core.publisher.Mono<Account> createAccount(String accountId, BigDecimal withAmount)
      throws HttpException {
    return getAccountResponse(accountId)
        .flatMap(
            accountResponse ->
                Mono.just(createAccountTransactionEnvelope(accountResponse, accountId, withAmount)))
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
              if (postResponse.status().code() >= 400) {
                return handleStellarPostError(postResponse, bodyBytesMono);
              }
              return bodyBytesMono.asString();
            })
        .flatMap(
            responseBody ->
                Mono.just(gson.fromJson(responseBody, SubmitTransactionResponse.class)));
  }

  private String createAccountTransactionEnvelope(
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

  public reactor.core.publisher.Mono<PaymentHistory> getAccountPaymentHistory(String accountID, @Nullable String beforeCursor, @Nullable String afterCursor)
      throws HttpException {
    return null;
  }

  public reactor.core.publisher.Mono<Payment> sendPayment(
      Account sourceAccount, Account destinationAccount, String currencyName, BigDecimal amount)
      throws HttpException {
    return null;
  }

  public reactor.core.publisher.Mono<DepositInstructions> getDepositInstructions(
      DepositRequirements config) throws HttpException {
    return null;
  }

  private <T> Mono<T> handleStellarPostError(
      HttpClientResponse response, ByteBufMono bodyBytesMono) {
    // TODO: implement
    return Mono.empty();
  }

  private <T> Mono<T> handleStellarGetError(
      HttpClientResponse response, ByteBufMono bodyBytesMono) {
    // TODO: implement
    return Mono.empty();
  }
}

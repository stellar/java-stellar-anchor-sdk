package org.stellar.anchor.paymentsservice.circle;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.netty.handler.codec.http.HttpHeaderNames;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.stellar.anchor.exception.HttpException;
import org.stellar.anchor.paymentsservice.*;
import org.stellar.anchor.paymentsservice.utils.NettyHttpClient;
import reactor.core.publisher.Mono;
import reactor.netty.ByteBufMono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.client.HttpClientResponse;
import reactor.util.annotation.NonNull;
import reactor.util.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
public class CirclePaymentsService implements PaymentsService {
    private static final Gson gson = new Gson();
    Network network = Network.CIRCLE;
    String url;
    String secretKey;

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private HttpClient _webClient;

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private String _mainAccountAddress;

    /**
     * For all service methods to work correctly, make sure your circle account has a valid business wallet and a bank
     * account configured.
     */
    public CirclePaymentsService() {
        super();
    }

    private HttpClient getWebClient(boolean authenticated) {
        if (_webClient == null) {
            _webClient = NettyHttpClient.withBaseUrl(getUrl());
        }
        if (!authenticated) {
            return _webClient;
        }
        return _webClient.headers(h -> h.add(HttpHeaderNames.AUTHORIZATION, "Bearer " + getSecretKey()));
    }

    @NonNull
    private <T> Mono<T> handleCircleError(HttpClientResponse response, ByteBufMono bodyBytesMono) {
        return bodyBytesMono
                .asString()
                .flatMap(body -> {
                    CircleError circleError = gson.fromJson(body, CircleError.class);
                    return Mono.error(new HttpException(
                            response.status().code(),
                            circleError.getMessage(),
                            circleError.getCode().toString()
                    ));
                });
    }

    /**
     * API request that pings the server to make sure it's up and running.
     *
     * @return asynchronous stream with a Void value. If no exception is thrown it means the request was successful and
     * the remote server is operational.
     * @throws HttpException If the http response status code is 4xx or 5xx.
     */
    public Mono<Void> ping() throws HttpException {
        return getWebClient(false)
                .get()
                .uri("/ping")
                .responseSingle((response, bodyBytesMono) -> {
                    if (response.status().code() >= 400) {
                        return handleCircleError(response, bodyBytesMono);
                    }

                    return Mono.empty();
                });
    }

    /**
     * API request that checks with the server if the provided secret key is valid and registered.
     *
     * @return asynchronous stream with a Void value. If no exception is thrown it means the request was successful and
     * the secret key is valid.
     * @throws HttpException If the http response status code is 4xx or 5xx.
     */
    public Mono<Void> validateSecretKey() throws HttpException {
        return getDistributionAccountAddress().flatMap(s -> Mono.empty());
    }

    /**
     * API request that returns the id of the distribution account managed by the secret key.
     *
     * @return asynchronous stream with the id of the distribution account managed by the secret key.
     * @throws HttpException If the http response status code is 4xx or 5xx.
     */
    public Mono<String> getDistributionAccountAddress() throws HttpException {
        if (_mainAccountAddress != null) {
            return Mono.just(_mainAccountAddress);
        }

        return getWebClient(true)
                .get()
                .uri("/v1/configuration")
                .responseSingle((response, bodyBytesMono) -> {
                    if (response.status().code() >= 400) {
                        return handleCircleError(response, bodyBytesMono);
                    }

                    return bodyBytesMono.asString();
                }).flatMap(body -> {
                    CircleConfigurationResponse response = gson.fromJson(body, CircleConfigurationResponse.class);
                    _mainAccountAddress = response.data.payments.masterWalletId;
                    return Mono.just(_mainAccountAddress);
                });
    }

    /**
     * Get the merchant account unsettled balances in circle
     *
     * @return asynchronous stream with a list of the unsettled balances.
     */
    @NonNull
    private Mono<List<Balance>> getMerchantAccountUnsettledBalances() {
        return getWebClient(true)
                .get()
                .uri("/v1/businessAccount/balances")
                .responseSingle((response, bodyBytesMono) -> {
                    if (response.status().code() >= 400) {
                        return handleCircleError(response, bodyBytesMono);
                    }

                    return bodyBytesMono.asString();
                }).flatMap(body -> {
                    CircleAccountBalancesResponse response = gson.fromJson(body, CircleAccountBalancesResponse.class);

                    List<Balance> unsettledBalances = new ArrayList<>();
                    for (CircleBalance uBalance : response.data.unsettled) {
                        unsettledBalances.add(uBalance.toBalance());
                    }

                    return Mono.just(unsettledBalances);
                });
    }

    /**
     * API request that retrieves the circle wallet with the given id.
     *
     * @param walletId is the existing wallet identifier.
     * @return asynchronous stream with the CircleWallet object.
     * @throws HttpException If the http response status code is 4xx or 5xx.
     */
    @NonNull
    private Mono<CircleWallet> getCircleWallet(String walletId) throws HttpException {
        return getWebClient(true)
                .get()
                .uri("/v1/wallets/" + walletId)
                .responseSingle((response, bodyBytesMono) -> {
                    if (response.status().code() >= 400) {
                        return handleCircleError(response, bodyBytesMono);
                    }

                    return bodyBytesMono.asString();
                })
                .flatMap(body -> {
                    CircleWalletResponse circleWalletResponse = gson.fromJson(body, CircleWalletResponse.class);
                    CircleWallet circleWallet = circleWalletResponse.data;
                    return Mono.just(circleWallet);
                });
    }

    /**
     * API request that retrieves the account with the given id.
     *
     * @param accountId is the existing account identifier of the circle account.
     * @return asynchronous stream with the account object.
     * @throws HttpException If the http response status code is 4xx or 5xx.
     */
    public Mono<Account> getAccount(String accountId) throws HttpException {
        return getDistributionAccountAddress()
                .flatMap(distAccountId -> {
                    Mono<List<Balance>> unsettledBalancesMono = Mono.just(new ArrayList<>());
                    if (distAccountId.equals(accountId)) {
                        unsettledBalancesMono = getMerchantAccountUnsettledBalances();
                    }
                    return Mono.zip(unsettledBalancesMono, getCircleWallet(accountId));
                }).flatMap(args -> {
                    List<Balance> unsettledBalances = args.getT1();
                    CircleWallet circleWallet = args.getT2();
                    Account account = circleWallet.toAccount();
                    account.setUnsettledBalances(unsettledBalances);
                    return Mono.just(account);
                });
    }

    /**
     * API request that creates an account with the given id.
     *
     * @param accountId is the identifier of the account to be created. It is used as an optional description in the
     *                  Circle implementation.
     * @return asynchronous stream with the account object.
     * @throws HttpException If the http response status code is 4xx or 5xx.
     */
    public Mono<Account> createAccount(@Nullable String accountId) throws HttpException {
        JsonObject postBody = new JsonObject();
        postBody.addProperty("idempotencyKey", UUID.randomUUID().toString());
        if (accountId != null && !accountId.isEmpty()) {
            postBody.addProperty("description", accountId);
        }

        return getWebClient(true)
                .post()
                .uri("/v1/wallets")
                .send(ByteBufMono.fromString(Mono.just(postBody.toString())))
                .responseSingle((postResponse, bodyBytesMono) -> {
                    if (postResponse.status().code() >= 400) {
                        return handleCircleError(postResponse, bodyBytesMono);
                    }

                    return bodyBytesMono.asString();
                }).flatMap(body -> {
                    CircleWalletResponse circleWalletResponse = gson.fromJson(body, CircleWalletResponse.class);
                    CircleWallet circleWallet = circleWalletResponse.data;
                    Account account = circleWallet.toAccount();
                    account.setUnsettledBalances(new ArrayList<>());
                    return Mono.just(account);
                });
    }

    /**
     * API request that returns the history of payments involving a given account.
     *
     * @param accountID the id of the account whose payment history we want to fetch.
     * @return asynchronous stream with the payment history.
     * @throws HttpException If the http response status code is 4xx or 5xx.
     */
    public Mono<PaymentHistory> getAccountPaymentHistory(String accountID) throws HttpException {
        // TODO: implement
        return null;
    }

    /**
     * API request that executes a payment between accounts. The APIKey needs to have access to the source account for
     * this request to succeed.
     *
     * @param sourceAccount      the account making the payment. Only the network and id fields are needed.
     * @param destinationAccount the account receiving the payment. The network field and a subset of (id, address and
     *                           addressTag) may be needed.
     * @param currencyName       the name of the currency used in the payment. It should obey the {scheme}:{identifier}
     *                           format described in <a href="https://stellar.org/protocol/sep-38#asset-identification-format">SEP-38</a>.
     * @param amount             the payment amount.
     * @return asynchronous stream with the payment object.
     * @throws HttpException If the provided input parameters are invalid.
     * @throws HttpException If the http response status code is 4xx or 5xx.
     */
    public Mono<Payment> sendPayment(Account sourceAccount, Account destinationAccount, String currencyName, Float amount) throws HttpException {
        // TODO: implement
        return null;
    }

    /**
     * API request that returns the info needed to make a deposit into a user account. This method will be needed if the
     * implementation allows users to make deposits using external networks. For instance, when a user wants to make a
     * deposit to their Circle account through a Stellar payment:
     *
     * <pre>{@code
     * // Here we want to check how we can top up a Circle account using USDC funds from the Stellar network.
     * String circleWalletId = "1000066041";
     * Network fromNetwork = Network.STELLAR;
     * String currencyName = "USD";  // or "USDC"
     * DepositConfiguration config = new DepositConfiguration(circleWalletId, fromNetwork, currencyName);
     *
     * // Here are the instructions with the Stellar account that will receive the payment:
     * DepositInfo depositInfo = getInfoForDeposit(config).block();
     * System.out.println("PublicKey: " + depositInfo.accountId);        // "PublicKey: G..."
     * System.out.println("Memo: " + depositInfo.accountIdTag);          // "Memo: 2454278437550473431"
     * System.out.println("Network: " + depositInfo.network);            // "Network: stellar"
     * System.out.println("CurrencyName: " + depositInfo.currencyName);  // "CurrencyName: stellar:USDC:<circle-issuer>"
     * System.out.println("Extra: " + depositInfo.extra);                // "Extra: null"
     * }</pre>
     *
     * @param config an object containing all configuration options needed for an external user to make a deposit to the
     *               desired internal account. Different fields may be mandatory depending on the interface
     *               implementation.
     * @return asynchronous stream with the info needed to make the deposit.
     * @throws HttpException If the http response status code is 4xx or 5xx or if the configuration is not
     *                       supported by the network.
     */
    public Mono<DepositInstructions> getDepositInstructions(DepositRequirements config) throws HttpException {
        // TODO: implement
        return null;
    }
}

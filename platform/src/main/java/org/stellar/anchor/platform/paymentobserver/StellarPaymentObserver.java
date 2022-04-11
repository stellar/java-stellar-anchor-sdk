package org.stellar.anchor.platform.paymentobserver;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import org.stellar.anchor.util.Log;
import org.stellar.sdk.Server;
import org.stellar.sdk.requests.EventListener;
import org.stellar.sdk.requests.PaymentsRequestBuilder;
import org.stellar.sdk.requests.SSEStream;
import org.stellar.sdk.responses.operations.OperationResponse;
import org.stellar.sdk.responses.operations.PaymentOperationResponse;
import shadow.com.google.common.base.Optional;

public class StellarPaymentObserver {
  final Server server;
  final List<PaymentListener> observers;
  final List<String> accounts;
  final List<SSEStream<OperationResponse>> streams;
  final PageTokenStore pageTokenStore;

  StellarPaymentObserver(
      String horizonServer,
      List<String> accounts,
      List<PaymentListener> observers,
      PageTokenStore pageTokenStore) {
    this.server = new Server(horizonServer);
    this.observers = observers;
    this.accounts = accounts;
    this.streams = new ArrayList<>(accounts.size());
    this.pageTokenStore = pageTokenStore;
  }

  /** Start watching the accounts. */
  public void start() {
    for (String account : accounts) {
      SSEStream<OperationResponse> stream = watch(account);
      this.streams.add(stream);
    }
  }

  /** Graceful shutdown. */
  public void shutdown() {
    for (SSEStream<OperationResponse> stream : streams) {
      stream.close();
    }
  }

  public SSEStream<OperationResponse> watch(String account) {
    PaymentsRequestBuilder paymentsRequest =
        server.payments().forAccount(account).includeTransactions(true);

    String lastToken = pageTokenStore.load(account);
    if (lastToken != null) {
      paymentsRequest.cursor(lastToken);
    }

    return paymentsRequest.stream(
        new EventListener<>() {
          @Override
          public void onEvent(OperationResponse transaction) {
            if (transaction instanceof PaymentOperationResponse) {
              PaymentOperationResponse payment = (PaymentOperationResponse) transaction;
              try {
                if (payment.getTo().equals(account)) {
                  observers.forEach(observer -> observer.onReceived(payment));
                } else if (payment.getFrom().equals(account)) {
                  observers.forEach(observer -> observer.onSent(payment));
                }
              } catch (Throwable t) {
                Log.errorEx(t);
              }
            }
            pageTokenStore.save(account, transaction.getPagingToken());
          }

          @Override
          public void onFailure(Optional<Throwable> exception, Optional<Integer> statusCode) {
            // TODO: The stream seems closed when failure happens. Improve the reliability of the
            // stream.
          }
        });
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    String horizonServer = "https://horizon-testnet.stellar.org";
    List<String> accounts = new LinkedList<>();
    List<PaymentListener> observers = new LinkedList<>();
    PageTokenStore pageTokenStore = new MemoryPageTokenStore();

    public Builder() {}

    public Builder horizonServer(String horizonServer) {
      this.horizonServer = horizonServer;
      return this;
    }

    public Builder addAccount(String account) {
      accounts.add(account);
      return this;
    }

    public Builder addObserver(PaymentListener observer) {
      observers.add(observer);
      return this;
    }

    public Builder paymentTokenStore(PageTokenStore pageTokenStore) {
      this.pageTokenStore = pageTokenStore;
      return this;
    }

    public StellarPaymentObserver build() {
      return new StellarPaymentObserver(horizonServer, accounts, observers, pageTokenStore);
    }
  }

  public static void main(String[] args) throws IOException, InterruptedException {
    KeyPair account1 =
        KeyPair.fromSecretSeed("SCBYEX2YH7BH5WVKVR22RW2M3QQR3P2P3NLWIVNNNEZHJ3KZ52E2QKZN");
    KeyPair account2 =
        KeyPair.fromSecretSeed("SDPJLASIYSGX7ZKOJZIGJGYGC5F6XKHTPEA7NR2Y6YKKCHIBR2GAPCEZ");

    StellarPaymentObserver watcher =
        builder()
            .horizonServer("https://horizon-testnet.stellar.org")
            .addAccount(account1.getAccountId())
            .addAccount(account2.getAccountId())
            .addObserver(
                new PaymentListener() {
                  @Override
                  public void onReceived(PaymentOperationResponse payment) {
                    System.out.println("Received:" + new Gson().toJson(payment));
                  }

                  @Override
                  public void onSent(PaymentOperationResponse payment) {
                    System.out.println("Sent:" + new Gson().toJson(payment));
                  }
                })
            .build();

    watcher.start();
    Thread.sleep(300000);
    watcher.shutdown();
  }
}

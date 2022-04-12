package org.stellar.anchor.platform.paymentobserver;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import org.stellar.anchor.util.Log;
import org.stellar.sdk.Server;
import org.stellar.sdk.requests.EventListener;
import org.stellar.sdk.requests.PaymentsRequestBuilder;
import org.stellar.sdk.requests.RequestBuilder;
import org.stellar.sdk.requests.SSEStream;
import org.stellar.sdk.responses.operations.OperationResponse;
import org.stellar.sdk.responses.operations.PaymentOperationResponse;
import shadow.com.google.common.base.Optional;

public class StellarPaymentObserver {
  final Server server;
  final List<PaymentListener> observers;
  final List<String> accounts;
  final List<SSEStream<OperationResponse>> streams;
  final PaymentStreamerCursorStore paymentStreamerCursorStore;

  StellarPaymentObserver(
      String horizonServer,
      List<String> accounts,
      List<PaymentListener> observers,
      PaymentStreamerCursorStore paymentStreamerCursorStore) {
    this.server = new Server(horizonServer);
    this.observers = observers;
    this.accounts = accounts;
    this.streams = new ArrayList<>(accounts.size());
    this.paymentStreamerCursorStore = paymentStreamerCursorStore;
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
        server.payments().forAccount(account).includeTransactions(true).limit(200).order(RequestBuilder.Order.DESC);

    String lastToken = paymentStreamerCursorStore.load(account);
    if (lastToken != null) {
      paymentsRequest.cursor(lastToken);
    }

    return paymentsRequest.stream(
        new EventListener<>() {
          @Override
          public void onEvent(OperationResponse operationResponse) {
            if (operationResponse instanceof PaymentOperationResponse) {
              PaymentOperationResponse payment = (PaymentOperationResponse) operationResponse;
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
            paymentStreamerCursorStore.save(account, operationResponse.getPagingToken());
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
    PaymentStreamerCursorStore paymentStreamerCursorStore = new MemoryPaymentStreamerCursorStore();

    public Builder() {}

    public Builder horizonServer(String horizonServer) {
      this.horizonServer = horizonServer;
      return this;
    }

    public Builder accounts(List<String> accounts) {
      this.accounts.addAll(accounts);
      return this;
    }

    public Builder observers(List<PaymentListener> observers) {
      this.observers.addAll(observers);
      return this;
    }

    public Builder paymentTokenStore(PaymentStreamerCursorStore paymentStreamerCursorStore) {
      this.paymentStreamerCursorStore = paymentStreamerCursorStore;
      return this;
    }

    public StellarPaymentObserver build() {
      return new StellarPaymentObserver(
          horizonServer, accounts, observers, paymentStreamerCursorStore);
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

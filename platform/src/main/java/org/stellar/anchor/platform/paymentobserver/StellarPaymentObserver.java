package org.stellar.anchor.platform.paymentobserver;

import static org.stellar.anchor.util.ReflectionUtil.getField;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.stellar.anchor.api.exception.SepException;
import org.stellar.anchor.api.platform.HealthCheckResult;
import org.stellar.anchor.platform.service.HealthCheckContext;
import org.stellar.anchor.platform.service.HealthCheckable;
import org.stellar.anchor.util.Log;
import org.stellar.sdk.Server;
import org.stellar.sdk.requests.EventListener;
import org.stellar.sdk.requests.PaymentsRequestBuilder;
import org.stellar.sdk.requests.RequestBuilder;
import org.stellar.sdk.requests.SSEStream;
import org.stellar.sdk.responses.operations.OperationResponse;
import org.stellar.sdk.responses.operations.PathPaymentBaseOperationResponse;
import org.stellar.sdk.responses.operations.PaymentOperationResponse;
import shadow.com.google.common.base.Optional;

public class StellarPaymentObserver implements HealthCheckable {
  final Server server;
  final List<PaymentListener> observers;
  final Collection<String> accounts;
  final List<SSEStream<OperationResponse>> streams;
  final PaymentStreamerCursorStore paymentStreamerCursorStore;
  final Map<SSEStream<OperationResponse>, String> mapStreamToAccount = new HashMap<>();

  StellarPaymentObserver(
      String horizonServer,
      Collection<String> accounts,
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
      mapStreamToAccount.put(stream, account);
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
        server
            .payments()
            .forAccount(account)
            .includeTransactions(true)
            .limit(200)
            .order(RequestBuilder.Order.ASC);
    String lastToken = paymentStreamerCursorStore.load(account);
    if (lastToken != null) {
      paymentsRequest.cursor(lastToken);
    }

    return paymentsRequest.stream(
        new EventListener<>() {
          @Override
          public void onEvent(OperationResponse operationResponse) {
            if (!operationResponse.isTransactionSuccessful()) {
              paymentStreamerCursorStore.save(account, operationResponse.getPagingToken());
              return;
            }

            ObservedPayment observedPayment = null;
            try {
              if (operationResponse instanceof PaymentOperationResponse) {
                PaymentOperationResponse payment = (PaymentOperationResponse) operationResponse;
                observedPayment = ObservedPayment.fromPaymentOperationResponse(payment);
              } else if (operationResponse instanceof PathPaymentBaseOperationResponse) {
                PathPaymentBaseOperationResponse pathPayment =
                    (PathPaymentBaseOperationResponse) operationResponse;
                observedPayment = ObservedPayment.fromPathPaymentOperationResponse(pathPayment);
              }
            } catch (SepException ex) {
              Log.warn(
                  String.format(
                      "Payment of id %s contains unsupported memo %s.",
                      operationResponse.getId(),
                      operationResponse.getTransaction().get().getMemo().toString()));
              Log.warnEx(ex);
            }

            if (observedPayment != null) {
              try {
                if (observedPayment.getTo().equals(account)) {
                  final ObservedPayment finalObservedPayment = observedPayment;
                  observers.forEach(observer -> observer.onReceived(finalObservedPayment));
                } else if (observedPayment.getFrom().equals(account)) {
                  final ObservedPayment finalObservedPayment = observedPayment;
                  observers.forEach(observer -> observer.onSent(finalObservedPayment));
                }
              } catch (Throwable t) {
                Log.errorEx(t);
              }
            }

            paymentStreamerCursorStore.save(account, operationResponse.getPagingToken());
          }

          @Override
          public void onFailure(Optional<Throwable> exception, Optional<Integer> statusCode) {
            Log.errorEx(exception.get());
            // TODO: The stream seems closed when failure happens. Improve the reliability of the
            // stream.
          }
        });
  }

  public static Builder builder() {
    return new Builder();
  }

  @Override
  public int compareTo(@NotNull HealthCheckable other) {
    return other.getName().compareTo(other.getName());
  }

  public static class Builder {
    String horizonServer = "https://horizon-testnet.stellar.org";
    Set<String> accounts = new HashSet<>();
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

  @Override
  public String getName() {
    return "stellar_payment_observer";
  }

  @Override
  public List<String> getTags() {
    return List.of("all", "event");
  }

  @Override
  public HealthCheckResult check(HealthCheckContext context) {
    List<StreamHealth> results = new ArrayList<>();
    for (SSEStream<OperationResponse> stream : streams) {
      StreamHealth.StreamHealthBuilder builder = StreamHealth.builder();
      builder.account(mapStreamToAccount.get(stream));
      // populate executorService information
      ExecutorService executorService = getField(stream, "executorService", null);
      if (executorService != null) {
        builder.threadShutdown(executorService.isShutdown());
        builder.threadTerminated(executorService.isTerminated());
      }

      builder.stopped(getField(stream, "isStopped", new AtomicBoolean(false)).get());

      AtomicReference<String> lastEventId = getField(stream, "lastEventId", null);
      if (lastEventId != null) {
        builder.lastEventId(lastEventId.get());
      }

      results.add(builder.build());
    }

    return new SPOHealthCheckResult(getName(), results);
  }
}

@AllArgsConstructor
class SPOHealthCheckResult implements HealthCheckResult {
  transient String name;

  List<StreamHealth> streams;

  public String name() {
    return name;
  }
}

@Data
@Builder
class StreamHealth {
  String account;
  boolean threadShutdown;
  boolean threadTerminated;
  boolean stopped;
  String lastEventId;
}

package org.stellar.anchor.platform.payment.observer.stellar;

import static org.stellar.anchor.api.platform.HealthCheckStatus.GREEN;
import static org.stellar.anchor.api.platform.HealthCheckStatus.RED;
import static org.stellar.anchor.util.ReflectionUtil.getField;

import com.google.gson.annotations.SerializedName;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import lombok.Builder;
import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.stellar.anchor.api.exception.SepException;
import org.stellar.anchor.api.exception.ValueValidationException;
import org.stellar.anchor.api.platform.HealthCheckResult;
import org.stellar.anchor.api.platform.HealthCheckStatus;
import org.stellar.anchor.healthcheck.HealthCheckable;
import org.stellar.anchor.platform.payment.observer.PaymentListener;
import org.stellar.anchor.platform.payment.observer.circle.ObservedPayment;
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
  final Set<PaymentListener> observers;
  final StellarPaymentStreamerCursorStore paymentStreamerCursorStore;
  final Map<SSEStream<OperationResponse>, String> mapStreamToAccount = new HashMap<>();
  final PaymentObservingAccountsManager paymentObservingAccountsManager;
  SSEStream<OperationResponse> stream;

  StellarPaymentObserver(
      String horizonServer,
      Set<PaymentListener> observers,
      PaymentObservingAccountsManager paymentObservingAccountsManager,
      StellarPaymentStreamerCursorStore paymentStreamerCursorStore) {
    this.server = new Server(horizonServer);
    this.observers = observers;
    this.paymentObservingAccountsManager = paymentObservingAccountsManager;
    this.paymentStreamerCursorStore = paymentStreamerCursorStore;
  }

  /** Start watching the accounts. */
  public void start() {
    this.stream = watch();
  }

  /** Graceful shutdown. */
  public void shutdown() {
    this.stream.close();
  }

  public SSEStream<OperationResponse> watch() {
    PaymentsRequestBuilder paymentsRequest =
        server.payments().includeTransactions(true).order(RequestBuilder.Order.ASC);
    String lastToken = paymentStreamerCursorStore.load();
    if (lastToken != null) {
      paymentsRequest.cursor(lastToken);
    }

    return paymentsRequest.stream(
        new EventListener<>() {
          @Override
          public void onEvent(OperationResponse operationResponse) {
            if (!operationResponse.isTransactionSuccessful()) {
              paymentStreamerCursorStore.save(operationResponse.getPagingToken());
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
                if (paymentObservingAccountsManager.lookupAndUpdate(observedPayment.getTo())) {
                  final ObservedPayment finalObservedPayment = observedPayment;
                  observers.forEach(observer -> observer.onReceived(finalObservedPayment));
                }
                if (paymentObservingAccountsManager.lookupAndUpdate(observedPayment.getFrom())
                    && !observedPayment.getTo().equals(observedPayment.getFrom())) {
                  final ObservedPayment finalObservedPayment = observedPayment;
                  observers.forEach(observer -> observer.onSent(finalObservedPayment));
                }
              } catch (Throwable t) {
                Log.errorEx(t);
              }
            }
            paymentStreamerCursorStore.save(operationResponse.getPagingToken());
          }

          @Override
          public void onFailure(Optional<Throwable> exception, Optional<Integer> statusCode) {
            Log.errorEx("stellar payment observer error: ", exception.get());
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
    Set<PaymentListener> observers = new HashSet<>();
    StellarPaymentStreamerCursorStore paymentStreamerCursorStore =
        new MemoryStellarPaymentStreamerCursorStore();
    private PaymentObservingAccountsManager paymentObservingAccountsManager;

    public Builder() {}

    public Builder horizonServer(String horizonServer) {
      this.horizonServer = horizonServer;
      return this;
    }

    public Builder observers(List<PaymentListener> observers) {
      this.observers.addAll(observers);
      return this;
    }

    public Builder paymentTokenStore(
        StellarPaymentStreamerCursorStore stellarPaymentStreamerCursorStore) {
      this.paymentStreamerCursorStore = stellarPaymentStreamerCursorStore;
      return this;
    }

    public Builder paymentObservingAccountManager(
        PaymentObservingAccountsManager paymentObservingAccountsManager) {
      this.paymentObservingAccountsManager = paymentObservingAccountsManager;
      return this;
    }

    public StellarPaymentObserver build() throws ValueValidationException {
      return new StellarPaymentObserver(
          horizonServer, observers, paymentObservingAccountsManager, paymentStreamerCursorStore);
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
  public HealthCheckResult check() {
    List<StreamHealth> results = new ArrayList<>();
    HealthCheckStatus status = GREEN;

    StreamHealth.StreamHealthBuilder healthBuilder = StreamHealth.builder();
    healthBuilder.account(mapStreamToAccount.get(stream));
    // populate executorService information
    ExecutorService executorService = getField(stream, "executorService", null);
    if (executorService != null) {
      healthBuilder.threadShutdown(executorService.isShutdown());
      healthBuilder.threadTerminated(executorService.isTerminated());
      if (executorService.isShutdown() || executorService.isTerminated()) {
        status = RED;
      }
    } else {
      status = RED;
    }

    boolean isStopped = getField(stream, "isStopped", new AtomicBoolean(false)).get();
    healthBuilder.stopped(isStopped);
    if (isStopped) {
      status = RED;
    }

    AtomicReference<String> lastEventId = getField(stream, "lastEventId", null);
    if (lastEventId != null) {
      healthBuilder.lastEventId(lastEventId.get());
    }

    results.add(healthBuilder.build());

    return SPOHealthCheckResult.builder()
        .name(getName())
        .streams(results)
        .status(status.getName())
        .build();
  }
}

/** The health check result of StellarPaymentObserver class. */
@Builder
@Data
class SPOHealthCheckResult implements HealthCheckResult {
  transient String name;

  List<String> statuses;

  String status;

  List<StreamHealth> streams;

  public String name() {
    return name;
  }
}

@Data
@Builder
class StreamHealth {
  String account;

  @SerializedName("thread_shutdown")
  boolean threadShutdown;

  @SerializedName("thread_terminated")
  boolean threadTerminated;

  boolean stopped;
  String lastEventId;
}

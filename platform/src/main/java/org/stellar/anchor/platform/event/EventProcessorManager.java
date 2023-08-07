package org.stellar.anchor.platform.event;

import static org.stellar.anchor.event.EventService.*;
import static org.stellar.anchor.util.Log.*;
import static org.stellar.anchor.util.MetricConstants.*;
import static org.stellar.anchor.util.MetricConstants.EVENT_RECEIVED;
import static org.stellar.anchor.util.StringHelper.json;

import io.micrometer.core.instrument.Metrics;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import lombok.SneakyThrows;
import org.stellar.anchor.api.event.AnchorEvent;
import org.stellar.anchor.api.exception.AnchorException;
import org.stellar.anchor.api.exception.InternalServerErrorException;
import org.stellar.anchor.asset.AssetService;
import org.stellar.anchor.config.SecretConfig;
import org.stellar.anchor.event.EventService;
import org.stellar.anchor.event.EventService.EventQueue;
import org.stellar.anchor.platform.config.CallbackApiConfig;
import org.stellar.anchor.platform.config.ClientsConfig;
import org.stellar.anchor.platform.config.EventProcessorConfig;
import org.stellar.anchor.platform.utils.DaemonExecutors;
import org.stellar.anchor.sep24.MoreInfoUrlConstructor;
import org.stellar.anchor.sep24.Sep24TransactionStore;
import org.stellar.anchor.util.ExponentialBackoffTimer;
import org.stellar.anchor.util.Log;

public class EventProcessorManager {
  public static final String CLIENT_STATUS_CALLBACK_EVENT_PROCESSOR_NAME_PREFIX =
      "client-status-callback-";
  public static final String CALLBACK_API_EVENT_PROCESSOR_NAME = "callback-api-";
  private final SecretConfig secretConfig;
  private final EventProcessorConfig eventProcessorConfig;
  private final CallbackApiConfig callbackApiConfig;
  private final ClientsConfig clientsConfig;
  private final EventService eventService;
  private final AssetService assetService;
  private final Sep24TransactionStore sep24TransactionStore;
  private final MoreInfoUrlConstructor moreInfoUrlConstructor;

  private final List<EventProcessor> processors = new ArrayList<>();

  public EventProcessorManager(
      SecretConfig secretConfig,
      EventProcessorConfig eventProcessorConfig,
      CallbackApiConfig callbackApiConfig,
      ClientsConfig clientsConfig,
      EventService eventService,
      AssetService assetService,
      Sep24TransactionStore sep24TransactionStore,
      MoreInfoUrlConstructor moreInfoUrlConstructor) {
    this.secretConfig = secretConfig;
    this.eventProcessorConfig = eventProcessorConfig;
    this.callbackApiConfig = callbackApiConfig;
    this.clientsConfig = clientsConfig;
    this.eventService = eventService;
    this.assetService = assetService;
    this.sep24TransactionStore = sep24TransactionStore;
    this.moreInfoUrlConstructor = moreInfoUrlConstructor;
  }

  @PostConstruct
  @SneakyThrows
  public void start() {
    if (eventProcessorConfig.getCallbackApiRequest().isEnabled()) {
      // Create a processor for the callback API handler
      processors.add(
          new EventProcessor(
              CALLBACK_API_EVENT_PROCESSOR_NAME,
              EventQueue.TRANSACTION,
              new CallbackApiEventHandler(callbackApiConfig)));
    }
    // Create a processor of the client status callback handler for each client defined in the
    // clientsConfig
    if (eventProcessorConfig.getClientStatusCallback().isEnabled()) {
      for (ClientsConfig.ClientConfig clientConfig : clientsConfig.getClients()) {
        if (clientConfig.getCallbackUrl().isEmpty()) {

          Log.info(String.format("Client status callback skipped: %s", json(clientConfig)));
          continue;
        }

        String processorName;
        switch (clientConfig.getType()) {
          case CUSTODIAL:
            processorName =
                CLIENT_STATUS_CALLBACK_EVENT_PROCESSOR_NAME_PREFIX + clientConfig.getSigningKey();
            break;
          case NONCUSTODIAL:
            processorName =
                CLIENT_STATUS_CALLBACK_EVENT_PROCESSOR_NAME_PREFIX + clientConfig.getDomain();
            break;
          default:
            errorF("Unknown client type: {}", clientConfig.getType());
            throw new InternalServerErrorException(
                "Unknown client type: " + clientConfig.getType());
        }
        processors.add(
            new EventProcessor(
                processorName,
                EventQueue.TRANSACTION,
                new ClientStatusCallbackHandler(
                    secretConfig,
                    clientConfig,
                    sep24TransactionStore,
                    assetService,
                    moreInfoUrlConstructor)));
      }
    }

    // Start all the processors
    processors.forEach(EventProcessor::start);
  }

  @PreDestroy
  public void stop() {
    for (EventProcessor processor : processors) {
      try {
        processor.stop();
      } catch (AnchorException ex) {
        errorF("Failed to shutdown event processor: {}}", processor.getClass());
        errorEx(ex);
      }
    }
  }

  class EventProcessor implements Runnable {
    // The initial backoff time for connection error.
    private final long NETWORK_INITIAL_BACKOFF_TIME_SECONDS = 1;
    // The maximum backoff time for connection error.
    private final long NETWORK_MAX_BACKOFF_TIME_SECONDS = 30;

    // The initial backoff time for HTTP status code other than 200s or 300s.
    private final long HTTP_STATUS_INITIAL_BACKOFF_TIME_SECONDS = 1;
    // The maximum backoff time for HTTP status code other than 200s or 300s.
    private final long HTTP_STATUS_MAX_BACKOFF_TIME_SECONDS = 5;
    // The maximum number of retries for HTTP status code other than 200s or 300s.
    private final long HTTP_STATUS_MAX_RETRIES = 3;

    private final String name;
    private final EventQueue eventQueue;
    private final EventHandler eventHandler;

    private final ScheduledExecutorService consumerScheduler =
        DaemonExecutors.newScheduledThreadPool(1);
    private ScheduledFuture<?> processingTask = null;
    private final ExponentialBackoffTimer networkBackoffTimer =
        new ExponentialBackoffTimer(
            NETWORK_INITIAL_BACKOFF_TIME_SECONDS, NETWORK_MAX_BACKOFF_TIME_SECONDS);
    private final ExponentialBackoffTimer httpErrorBackoffTimer =
        new ExponentialBackoffTimer(
            HTTP_STATUS_INITIAL_BACKOFF_TIME_SECONDS, HTTP_STATUS_MAX_BACKOFF_TIME_SECONDS);

    // The flag to indicate if the processor is stopped.
    private boolean stopped = false;

    public EventProcessor(String name, EventQueue eventQueue, EventHandler eventHandler) {
      this.name = name;
      this.eventQueue = eventQueue;
      this.eventHandler = eventHandler;
    }

    public void start() {
      processingTask = consumerScheduler.scheduleWithFixedDelay(this, 1, 2, TimeUnit.SECONDS);
    }

    public void stop() throws AnchorException {
      stopped = true;

      if (processingTask != null) {
        processingTask.cancel(true);
      }

      consumerScheduler.shutdown();
    }

    @SneakyThrows
    @Override
    public void run() {
      infoF(
          "The EventProcessor listening task is starting for the {} time.",
          getConsumerRestartCount() + 1);
      Session queueSession = eventService.createSession(name, eventQueue);
      try {
        while (!Thread.currentThread().isInterrupted() && !stopped) {
          ReadResponse readResponse = queueSession.read();
          List<AnchorEvent> events = readResponse.getEvents();
          Metrics.counter(EVENT_RECEIVED, QUEUE, toMetricTag(eventQueue.name()))
              .increment(events.size());
          debugF("Received {} events from queue", events.size());
          // The event delivery should retry in one of the 3 ways:
          //
          // Error #1: If connection error (IOException) happens, the events will be retried with
          // exponential
          // backoff timer. All subsequent events will NOT be delivered until the connection becomes
          // successful.
          //
          // Error #2: If the business server returns HTTP status code other than 200s or 300s, we
          // will retry [3] times
          // with backoff timer. After 3 retries, we will skip the event and proceed to next
          // sub-sequent events.
          //
          // Error #3: If the event processor encounters an un-expected error caused by the un-seen
          // bugs, the
          // event will be delivered to a no-op DLQ class which has Log.error implementation.
          events.forEach(
              event -> {
                boolean isProcessed = false;
                int httpStatusNotOkAttempts = 0;
                // For every event, reset the timer.
                httpErrorBackoffTimer.reset();
                networkBackoffTimer.reset();
                // Retry until the event is processed or the thread is interrupted.
                while (!isProcessed || !Thread.currentThread().isInterrupted()) {
                  try {
                    if (eventHandler.handleEvent(event)) {
                      // ***** The event is processed successfully.
                      isProcessed = true;
                      Metrics.counter(EVENT_PROCESSED, QUEUE, toMetricTag(eventQueue.name()))
                          .increment(events.size());
                    } else {
                      // ***** Error #2. HTTP status code other than 200s or 300s
                      networkBackoffTimer.reset();
                      if (httpStatusNotOkAttempts < HTTP_STATUS_MAX_RETRIES) {
                        // retry.
                        httpStatusNotOkAttempts++;
                        httpErrorBackoffTimer.backoff();
                      } else {
                        // retry > 3 times, skip the event.
                        isProcessed = true;
                      }
                    }
                  } catch (IOException ioex) {
                    // ***** Error #1: connection error
                    httpErrorBackoffTimer.reset();
                    httpStatusNotOkAttempts = 0;
                    try {
                      networkBackoffTimer.backoff();
                    } catch (InterruptedException e) {
                      // The thread is interrupted, so we need to stop the processor. This will
                      // break the while loop.
                      isProcessed = false;
                    }
                  } catch (Exception e) {
                    // ***** Error #3. uncaught exception
                    networkBackoffTimer.reset();
                    httpErrorBackoffTimer.reset();
                    sendToDLQ(event, e);
                    isProcessed = true;
                  }
                }
              });
          // Do not continue if the thread is interrupted.
          if (Thread.currentThread().isInterrupted()) break;
          queueSession.ack(readResponse);
        }

        queueSession.close();
      } catch (Exception ex) {
        // This is unexpected, so we need to restart the consumer.
        Log.errorEx(ex);
      } finally {
        queueSession.close();
        infoF("Closing queue session [{}]", queueSession.getSessionName());
      }
    }

    private void sendToDLQ(AnchorEvent event, Exception e) {
      Log.errorEx(e);
    }

    long getConsumerRestartCount() {
      return ((ScheduledThreadPoolExecutor) consumerScheduler).getCompletedTaskCount();
    }
  }

  private String toMetricTag(String name) {
    switch (name) {
      case CALLBACK_API_EVENT_PROCESSOR_NAME:
        return TV_BUSINESS_SERVER_CALLBACK;
      case CLIENT_STATUS_CALLBACK_EVENT_PROCESSOR_NAME_PREFIX:
        return TV_STATUS_CALLBACK;
      default:
        return TV_UNKNOWN;
    }
  }
}

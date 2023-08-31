package org.stellar.anchor.platform.event;

import static java.lang.Thread.currentThread;
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
import org.stellar.anchor.sep31.Sep31TransactionStore;
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
  private final Sep31TransactionStore sep31TransactionStore;
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
      Sep31TransactionStore sep31TransactionStore,
      MoreInfoUrlConstructor moreInfoUrlConstructor) {
    this.secretConfig = secretConfig;
    this.eventProcessorConfig = eventProcessorConfig;
    this.callbackApiConfig = callbackApiConfig;
    this.clientsConfig = clientsConfig;
    this.eventService = eventService;
    this.assetService = assetService;
    this.sep24TransactionStore = sep24TransactionStore;
    this.sep31TransactionStore = sep31TransactionStore;
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
              new CallbackApiEventHandler(callbackApiConfig),
              eventService));
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
                    sep31TransactionStore,
                    assetService,
                    moreInfoUrlConstructor),
                eventService));
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

  static class EventProcessor implements Runnable {
    // The initial backoff time for connection error.
    private final long NETWORK_INITIAL_BACKOFF_TIME_SECONDS = 1;
    // The maximum backoff time for connection error.
    private final long NETWORK_MAX_BACKOFF_TIME_SECONDS = 30;
    // The initial backoff time for HTTP status code other than 200s or 300s.
    private final long HTTP_STATUS_INITIAL_BACKOFF_TIME_SECONDS = 1;
    // The maximum backoff time for HTTP status code other than 200s or 300s.
    private final long HTTP_STATUS_MAX_BACKOFF_TIME_SECONDS = 5;
    // The maximum number of retries for HTTP status code other than 200s or 300s.
    private final long MAX_RETRIES = 3;

    private final String name;
    private final EventQueue eventQueue;
    private final EventHandler eventHandler;
    private final EventService eventService;

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

    public EventProcessor(
        String name, EventQueue eventQueue, EventHandler eventHandler, EventService eventService) {
      this.name = name;
      this.eventQueue = eventQueue;
      this.eventHandler = eventHandler;
      this.eventService = eventService;
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
        while (!currentThread().isInterrupted() && !stopped) {
          ReadResponse readResponse = queueSession.read();
          List<AnchorEvent> events = readResponse.getEvents();
          Metrics.counter(EVENT_RECEIVED, QUEUE, toMetricTag(eventQueue.name()))
              .increment(events.size());
          debugF("Received {} events from queue", events.size());
          for (AnchorEvent event : events) {
            handleEvent(event);
            if (currentThread().isInterrupted()) break;
          }
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

    void handleEvent(AnchorEvent event) {
      boolean isProcessed = false;
      int retryAttempts = 0;
      // For every event, reset the timer.
      getHttpErrorBackoffTimer().reset();
      getNetworkBackoffTimer().reset();
      // Retry until the event is processed or the thread is interrupted.
      while (!isProcessed && !currentThread().isInterrupted()) {
        try {
          if (eventHandler.handleEvent(event)) {
            // ***** The event is processed successfully.
            isProcessed = true;
            incrementProcessedCounter();
          } else {
            // ***** Error #2. HTTP status code other than 200s or 300s
            if (++retryAttempts < MAX_RETRIES) {
              // retry.
              try {
                getHttpErrorBackoffTimer().backoff();
              } catch (InterruptedException e) {
                // The thread is interrupted, so we need to stop the processor. This will
                // break the while loop.
                currentThread().interrupt();
              }
            } else {
              // retry >= 3 times, skip the event.
              isProcessed = true;
              incrementProcessedCounter();
            }
          }
        } catch (IOException ioex) {
          // Retry for connection error
          if (++retryAttempts < MAX_RETRIES) {
            try {
              getNetworkBackoffTimer().backoff();
            } catch (InterruptedException e) {
              // The thread is interrupted, so we need to stop the processor. This will
              // break the while loop.
              currentThread().interrupt();
            }
          } else {
            isProcessed = true;
            incrementProcessedCounter();
          }
        } catch (Exception e) {
          // ***** Error #3. uncaught exception
          sendToDLQ(event, e);
          isProcessed = true;
        }
      }
    }

    void incrementProcessedCounter() {
      Metrics.counter(EVENT_PROCESSED, QUEUE, toMetricTag(eventQueue.name())).increment();
    }

    void sendToDLQ(AnchorEvent event, Exception e) {
      Log.errorF("Failed to process event: {}", json(event));
      Log.errorEx(e);
    }

    long getConsumerRestartCount() {
      return ((ScheduledThreadPoolExecutor) consumerScheduler).getCompletedTaskCount();
    }

    ExponentialBackoffTimer getHttpErrorBackoffTimer() {
      return httpErrorBackoffTimer;
    }

    ExponentialBackoffTimer getNetworkBackoffTimer() {
      return networkBackoffTimer;
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
}

package org.stellar.anchor.platform.event;

import static org.stellar.anchor.event.EventService.*;
import static org.stellar.anchor.util.Log.*;
import static org.stellar.anchor.util.StringHelper.json;

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
import org.stellar.anchor.event.EventService;
import org.stellar.anchor.event.EventService.EventQueue;
import org.stellar.anchor.platform.config.CallbackApiConfig;
import org.stellar.anchor.platform.config.ClientsConfig;
import org.stellar.anchor.platform.config.EventProcessorConfig;
import org.stellar.anchor.platform.utils.DaemonExecutors;
import org.stellar.anchor.util.Log;

public class EventProcessorManager {
  public static final String CLIENT_STATUS_CALLBACK_EVENT_PROCESSOR_NAME_PREFIX =
      "client-status-callback-";
  public static final String CALLBACK_API_EVENT_PROCESSOR_NAME = "callback-api-";
  private final EventProcessorConfig eventProcessorConfig;
  private final CallbackApiConfig callbackApiConfig;
  private final ClientsConfig clientsConfig;
  private final EventService eventService;

  private final List<EventProcessor> processors = new ArrayList<>();

  public EventProcessorManager(
      EventProcessorConfig eventProcessorConfig,
      CallbackApiConfig callbackApiConfig,
      ClientsConfig clientsConfig,
      EventService eventService) {
    this.eventProcessorConfig = eventProcessorConfig;
    this.callbackApiConfig = callbackApiConfig;
    this.clientsConfig = clientsConfig;
    this.eventService = eventService;
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
                  new ClientStatusCallbackHandler(clientConfig)));
        }
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
    private final String name;
    private final EventQueue eventQueue;
    private final EventHandler eventHandler;

    private final ScheduledExecutorService consumerScheduler =
        DaemonExecutors.newScheduledThreadPool(1);
    private ScheduledFuture<?> processingTask = null;
    private boolean stopped = false;
    private static final int INITIAL_DELAY = 1000; // Initial delay in milliseconds

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
          debugF("Received {} events from queue", events.size());
          events.forEach(
              event -> {
                // TODO: Implement retry mechanism here
                boolean isProcessed = false;
                int attempts = 0;
                while (!isProcessed) {
                  try {
                    eventHandler.handleEvent(event);
                    isProcessed = true;
                  } catch (Exception e) {
                    Log.errorEx(e);
                    backoffTimer(attempts++);
                    // TODO: handle retry according to response
                    // if downstream cannot consume event -> keep retry
                    // if it's code issue -> event goes to dead letter queue
                  }
                }
              });
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

    private void backoffTimer(int attempts) {
      int delay = (int) (Math.pow(2, attempts) * INITIAL_DELAY);
      try {
        Thread.sleep(delay);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }

    long getConsumerRestartCount() {
      return ((ScheduledThreadPoolExecutor) consumerScheduler).getCompletedTaskCount();
    }
  }
}

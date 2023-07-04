package org.stellar.anchor.platform.event;

import static org.stellar.anchor.util.Log.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import lombok.SneakyThrows;
import org.stellar.anchor.api.event.AnchorEvent;
import org.stellar.anchor.api.exception.AnchorException;
import org.stellar.anchor.event.EventService;
import org.stellar.anchor.platform.config.CallbackApiConfig;
import org.stellar.anchor.platform.config.ClientsConfig;
import org.stellar.anchor.platform.config.EventProcessorConfig;
import org.stellar.anchor.util.Log;

public class EventProcessorManager {
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
  public void start() {
    if (eventProcessorConfig.getCallbackApiRequest().isEnabled()) {
      // Add callback API event processor
      processors.add(
          new EventProcessor(
              EventService.EventQueue.TRANSACTION, new CallbackApiHandler(callbackApiConfig)));
    }

    if (eventProcessorConfig.getClientStatusCallback().isEnabled()) {
      // Create client status callback event processors
      clientsConfig
          .getClients()
          .forEach(
              client ->
                  processors.add(
                      new EventProcessor(
                          EventService.EventQueue.TRANSACTION,
                          new ClientStatusCallbackHandler(client))));
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
    private final EventService.Session queueSession;
    private final EventHandler eventHandler;
    private final ScheduledExecutorService consumerScheduler = Executors.newScheduledThreadPool(1);

    public EventProcessor(EventService.EventQueue eventQueue, EventHandler eventHandler) {
      this.eventHandler = eventHandler;
      this.queueSession = eventService.createSession(eventQueue);
    }

    public void start() {
      consumerScheduler.scheduleWithFixedDelay(this, 1, 2, TimeUnit.SECONDS);
    }

    public void stop() throws AnchorException {
      consumerScheduler.shutdown();
      queueSession.close();
    }

    @SneakyThrows
    @Override
    public void run() {
      try {
        infoF(
            "The EventProcessor Kafka listening task is starting for the {} time.",
            getConsumerRestartCount() + 1);

        while (!Thread.currentThread().isInterrupted()) {
          EventService.ReadResponse readResponse = queueSession.read();
          List<AnchorEvent> events = readResponse.getEvents();
          debugF("Received %s events from queue%n", events.size());
          events.forEach(eventHandler::handleEvent);
          queueSession.ack(readResponse);
        }
      } catch (Exception ex) {
        // This is unexpected, so we need to restart the consumer.
        Log.errorEx(ex);
      }
    }

    long getConsumerRestartCount() {
      return ((ScheduledThreadPoolExecutor) consumerScheduler).getCompletedTaskCount();
    }
  }
}

package org.stellar.anchor.reference.event;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.AmazonSQSAsyncClientBuilder;
import com.amazonaws.services.sqs.model.*;
import com.google.gson.Gson;
import java.util.List;
import java.util.concurrent.*;
import javax.annotation.PreDestroy;
import org.stellar.anchor.event.models.QuoteEvent;
import org.stellar.anchor.event.models.TransactionEvent;
import org.stellar.anchor.reference.config.SqsListenerSettings;
import org.stellar.anchor.util.Log;

public class SqsListener extends AbstractEventListener {
  private final AnchorEventProcessor processor;
  private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(5);
  private final AmazonSQSAsync sqsClient;

  public SqsListener(SqsListenerSettings sqsListenerSettings, AnchorEventProcessor processor) {
    String key = sqsListenerSettings.getAccessKey();
    String region = sqsListenerSettings.getRegion();
    if (key != null) {
      Log.debugF("sqs listener access key is {}", sqsListenerSettings.getAccessKey());
    } else {
      Log.debug("sqs access key is null");
    }
    if (region != null) {
      Log.debugF("sqs listener region is {}", sqsListenerSettings.getRegion());
    } else {
      Log.debug("sqs region is null");
    }
    this.processor = processor;
    this.sqsClient =
        AmazonSQSAsyncClientBuilder.standard()
            .withRegion(sqsListenerSettings.getRegion())
            .withCredentials(
                new AWSStaticCredentialsProvider(
                    new BasicAWSCredentials(
                        sqsListenerSettings.getAccessKey(), sqsListenerSettings.getSecretKey())))
            .build();
    SqsListenerSettings.Queues q = sqsListenerSettings.getEventTypeToQueue();
    if (sqsListenerSettings.isUseSingleQueue()) {
      executor.scheduleAtFixedRate(new SqsListen(q.getAll()), 0, 10, TimeUnit.SECONDS);
    } else {
      executor.scheduleAtFixedRate(
          new SqsListen(q.getTransactionStatusChanged()), 0, 10, TimeUnit.SECONDS);
      executor.scheduleAtFixedRate(new SqsListen(q.getQuoteCreated()), 0, 10, TimeUnit.SECONDS);
      executor.scheduleAtFixedRate(
          new SqsListen(q.getTransactionCreated()), 0, 10, TimeUnit.SECONDS);
      executor.scheduleAtFixedRate(new SqsListen(q.getTransactionError()), 0, 10, TimeUnit.SECONDS);
    }
  }

  public List<Message> getSqsMessages(String queueUrl) {
    ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest();
    receiveMessageRequest.setQueueUrl(queueUrl);
    receiveMessageRequest.setMaxNumberOfMessages(
        10); // SQS supports a max of 10 messages per request
    receiveMessageRequest.withMessageAttributeNames("anchor-event-class");
    ReceiveMessageResult receiveMessageResult = sqsClient.receiveMessage(receiveMessageRequest);
    return receiveMessageResult.getMessages();
  }

  class SqsListen implements Runnable {
    private final String queue;

    public SqsListen(String queue) {
      this.queue = queue;
    }

    public void run() {
      Log.debug("SQS event consumer started for queue: " + queue);
      Gson gson = new Gson();
      try {
        String queueUrl = sqsClient.getQueueUrl(queue).getQueueUrl();
        List<Message> messages = getSqsMessages(queue);
        while (messages.size() > 0) {

          for (Message message : messages) {
            String eventClass =
                message.getMessageAttributes().get("anchor-event-class").getStringValue();
            switch (eventClass) {
              case "QuoteEvent":
                QuoteEvent quoteEvent = gson.fromJson(message.getBody(), QuoteEvent.class);
                Log.debug("new quote event from sqs - " + quoteEvent.getEventId());
                processor.handleQuoteEvent(quoteEvent);
                break;
              case "TransactionEvent":
                TransactionEvent trxEvent =
                    gson.fromJson(message.getBody(), TransactionEvent.class);
                processor.handleTransactionEvent(trxEvent);
                break;
              default:
                Log.debug("error: anchor_platform_event - invalid message type '%s'%n", eventClass);
            }
            sqsClient.deleteMessage(queueUrl, message.getReceiptHandle());
          }
          if (Thread.interrupted()) {
            break;
          }
          messages = getSqsMessages(queueUrl);
        }
      } catch (Exception ex) {
        Log.errorEx(ex);
      }
    }
  }

  public void stop() {
    executor.shutdownNow();
  }

  @PreDestroy
  public void destroy() {
    stop();
  }
}

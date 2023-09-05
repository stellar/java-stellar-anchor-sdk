package org.stellar.anchor.reference.event;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.AmazonSQSAsyncClientBuilder;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import com.google.gson.Gson;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import org.stellar.anchor.api.event.AnchorEvent;
import org.stellar.anchor.reference.config.SqsListenerSettings;
import org.stellar.anchor.util.Log;

public class SqsListener extends AbstractEventListener {
  private final SqsListenerSettings sqsListenerSettings;
  private final AnchorEventProcessor processor;
  private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(5);
  private AmazonSQSAsync sqsClient;

  public SqsListener(SqsListenerSettings sqsListenerSettings, AnchorEventProcessor processor) {
    this.sqsListenerSettings = sqsListenerSettings;
    this.processor = processor;
  }

  @PostConstruct
  public void start() {
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

  @PreDestroy
  public void stop() throws InterruptedException {
    executor.shutdownNow();
    executor.awaitTermination(10, TimeUnit.SECONDS);
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
            AnchorEvent event = gson.fromJson(message.getBody(), AnchorEvent.class);
            processor.handleEvent(event);
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
}

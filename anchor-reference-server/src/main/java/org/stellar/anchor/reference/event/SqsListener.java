package org.stellar.anchor.reference.event;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.AmazonSQSAsyncClientBuilder;
import com.amazonaws.services.sqs.model.*;
import com.google.gson.Gson;
import org.stellar.anchor.event.models.QuoteEvent;
import org.stellar.anchor.event.models.TransactionEvent;
import org.stellar.anchor.reference.config.SqsListenerSettings;
import org.stellar.anchor.util.Log;

import javax.annotation.PreDestroy;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SqsListener extends AbstractEventListener{
    private final SqsListenerSettings sqsListenerSettings;
    private final AnchorEventProcessor processor;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final AmazonSQSAsync sqsClient;

    public SqsListener(
            SqsListenerSettings sqsListenerSettings, AnchorEventProcessor processor) {
        this.sqsListenerSettings = sqsListenerSettings;
        this.processor = processor;
        this.executor.submit(this::listen);
        this.sqsClient =
                AmazonSQSAsyncClientBuilder
                        .standard()
                        .withRegion(sqsListenerSettings.getRegion())
                        .withCredentials(new AWSStaticCredentialsProvider(
                                new BasicAWSCredentials(sqsListenerSettings.getAccessKey(),
                                        sqsListenerSettings.getSecretKey())))
                        .build();
    }

    public void listen() {
        Log.info("SQS queue consumer server started ");
        SqsListenerSettings.Queues q = sqsListenerSettings.getQueues();
        Gson gson = new Gson();
        while (!Thread.interrupted()) {
            try {
                String queueUrl = sqsClient.getQueueUrl(q.getTransactionPaymentReceived()).getQueueUrl();
                ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest();
                receiveMessageRequest.setQueueUrl(queueUrl);
                receiveMessageRequest.setWaitTimeSeconds(5); // Listener for messages in the next 5 seconds
                receiveMessageRequest.setMaxNumberOfMessages(10); // SQS supports a max of 10 messages per request
                receiveMessageRequest.withMessageAttributeNames("anchor-event-class");
                ReceiveMessageResult receiveMessageResult = sqsClient.receiveMessage(receiveMessageRequest);
                List<Message> messages = receiveMessageResult.getMessages();
                for (Message message: messages){
                    String eventClass = message.getMessageAttributes().get("anchor-event-class").getStringValue();
                    switch(eventClass){
                        case "QuoteEvent":
                            QuoteEvent quoteEvent = gson.fromJson(message.getBody(), QuoteEvent.class);
                            Log.debug("new quote event from sqs - " + quoteEvent.getEventId());
                            processor.handleQuoteEvent(quoteEvent);
                            break;
                        case "TransactionEvent":
                            TransactionEvent trxEvent = gson.fromJson(message.getBody(), TransactionEvent.class);
                            processor.handleTransactionEvent(trxEvent);
                            break;
                        default:
                            Log.debug(
                                    "error: anchor_platform_event - invalid message type '%s'%n", eventClass);
                    }
                    sqsClient.deleteMessage(queueUrl, message.getReceiptHandle());
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

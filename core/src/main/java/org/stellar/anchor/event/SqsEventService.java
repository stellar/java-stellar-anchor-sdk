package org.stellar.anchor.event;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.sqs.AmazonSQSAsyncClientBuilder;
//import com.amazonaws.services.sqs.model.Message;
import com.google.gson.Gson;
import org.springframework.messaging.Message;
import org.springframework.cloud.aws.messaging.core.QueueMessagingTemplate;

import org.stellar.anchor.config.EventConfig;
import org.stellar.anchor.config.SqsConfig;
import org.stellar.anchor.event.models.AnchorEvent;
import org.stellar.anchor.event.models.QuoteEvent;
import org.stellar.anchor.util.Log;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SqsEventService implements EventPublishService{
    final QueueMessagingTemplate sqsClient;
    final Map<String, String> eventTypeToQueue;
    final boolean eventsEnabled;
    final boolean useSingleQueue;

    public SqsEventService(EventConfig eventConfig, SqsConfig sqsConfig) {
        sqsClient = new QueueMessagingTemplate(
                AmazonSQSAsyncClientBuilder
                    .standard()
                    .withRegion(sqsConfig.getRegion())
                    .withCredentials(new AWSStaticCredentialsProvider(
                            new BasicAWSCredentials(sqsConfig.getAccessKey(), sqsConfig.getSecretKey())))
                    .build()
            );

        this.eventTypeToQueue = sqsConfig.getEventTypeToQueue();
        this.eventsEnabled = eventConfig.isEnabled();
        this.useSingleQueue = eventConfig.isUseSingleQueue();
    }

    public void publish(AnchorEvent event) {
        try {
            String queue;
            if (useSingleQueue) {
                queue = eventTypeToQueue.get("all");
            } else {
                queue = eventTypeToQueue.get(event.getType());
            }
            Map<String, Object> headers = new HashMap<>();
            headers.put("message-group-id", queue);
            headers.put("message-deduplication-id", event.getEventId());
            headers.put("anchor-message-type", event.getClass().getSimpleName());
            Gson gson = new Gson();
            String evntStr = gson.toJson(event);
            sqsClient.convertAndSend(queue, evntStr, headers);

            Message message = sqsClient.receive(queue);
            switch(message.getHeaders().get("anchor-message-type").toString()){
                case "QuoteEvent":
                    //Gson gson = new Gson();
                    QuoteEvent event1 = gson.fromJson(message.getPayload().toString(), QuoteEvent.class);
                    System.out.println("new quote event from sqs - " + event1.getEventId());
            } 

        } catch (Exception ex) {
            Log.errorEx(ex);
        }

    }
}

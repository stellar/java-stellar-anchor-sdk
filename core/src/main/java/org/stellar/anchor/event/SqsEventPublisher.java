package org.stellar.anchor.event;

import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.AmazonSQSAsyncClientBuilder;
import com.amazonaws.services.sqs.model.MessageAttributeValue;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.google.gson.Gson;
import org.stellar.anchor.config.event.SqsConfig;
import org.stellar.anchor.event.models.AnchorEvent;
import org.stellar.anchor.util.Log;

public class SqsEventPublisher extends EventPublisher {
  final AmazonSQSAsync sqsClient;

  public SqsEventPublisher(SqsConfig sqsConfig) {
    this.sqsClient =
        AmazonSQSAsyncClientBuilder.standard().withRegion(sqsConfig.getAwsRegion()).build();
  }

  @Override
  public void publish(AnchorEvent event) {
    try {
      // TODO implement batching
      // TODO retry logic?
      String queue = eventService.getQueue(event.getType());
      Gson gson = new Gson();
      String eventStr = gson.toJson(event);

      String queueUrl = sqsClient.getQueueUrl(queue).getQueueUrl();
      SendMessageRequest sendMessageRequest =
          new SendMessageRequest()
              .withQueueUrl(queueUrl)
              .withMessageBody(eventStr)
              .withMessageGroupId(queue)
              .withMessageDeduplicationId(event.getEventId());

      sendMessageRequest.addMessageAttributesEntry(
          "anchor-event-class",
          new MessageAttributeValue()
              .withDataType("String")
              .withStringValue(event.getClass().getSimpleName()));
      sqsClient.sendMessage(sendMessageRequest);

    } catch (Exception ex) {
      Log.errorEx(ex);
    }
  }
}

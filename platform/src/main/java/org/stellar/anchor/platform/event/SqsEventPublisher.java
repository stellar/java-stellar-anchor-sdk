package org.stellar.anchor.platform.event;

import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.AmazonSQSAsyncClientBuilder;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageResult;
import com.google.gson.Gson;
import org.stellar.anchor.api.event.AnchorEvent;
import org.stellar.anchor.api.exception.EventPublishException;
import org.stellar.anchor.platform.config.SqsConfig;
import org.stellar.anchor.util.Log;

public class SqsEventPublisher implements EventPublisher {
  final AmazonSQSAsync sqsClient;

  public SqsEventPublisher(SqsConfig sqsConfig) {
    this.sqsClient =
        AmazonSQSAsyncClientBuilder.standard().withRegion(sqsConfig.getAwsRegion()).build();
  }

  @Override
  public void publish(String queue, AnchorEvent event) throws EventPublishException {
    try {
      // TODO implement batching
      // TODO retry logic?
      Gson gson = new Gson();
      String eventStr = gson.toJson(event);

      String queueUrl = sqsClient.getQueueUrl(queue).getQueueUrl();
      SendMessageRequest sendMessageRequest =
          new SendMessageRequest()
              .withQueueUrl(queueUrl)
              .withMessageBody(eventStr)
              .withMessageGroupId(queue)
              .withMessageDeduplicationId(event.getId());

      SendMessageResult sendMessageResult = sqsClient.sendMessage(sendMessageRequest);

      // If the queue is offline, throw an exception
      int statusCode = sendMessageResult.getSdkHttpMetadata().getHttpStatusCode();
      if (statusCode < 200 || statusCode > 299) {
        Log.error("failed to send message to SQS");
        throw new EventPublishException(
            String.format(
                "Failed to publish event to AWS SQS. [StatusCode: %d] [Metadata: %s]",
                statusCode, sendMessageResult.getSdkHttpMetadata().toString()));
      }
    } catch (EventPublishException ex) {
      throw ex;
    } catch (Exception ex) {
      Log.errorEx(ex);
    }
  }
}

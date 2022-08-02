package org.stellar.anchor.platform.payment.observer.circle.model;

import com.google.gson.annotations.SerializedName;
import java.time.Instant;
import java.util.Map;
import lombok.Data;

@Data
public class CircleNotification {
  @SerializedName("Type")
  String type;

  @SerializedName("MessageId")
  String messageId;

  @SerializedName("Message")
  String message;

  @SerializedName("TopicArn")
  String topicArn;

  @SerializedName("Timestamp")
  Instant Timestamp;

  @SerializedName("SignatureVersion")
  String signatureVersion;

  @SerializedName("Signature")
  String signature;

  @SerializedName("SigningCertURL")
  String signingCertURL;

  // For Type == "SubscriptionConfirmation"
  @SerializedName("Token")
  String token;

  @SerializedName("SubscribeURL")
  String subscribeUrl;

  // For Type == "Notification"
  @SerializedName("UnsubscribeURL")
  String unsubscribeURL;

  @SerializedName("MessageAttributes")
  Map<String, ?> messageAttributes;
}

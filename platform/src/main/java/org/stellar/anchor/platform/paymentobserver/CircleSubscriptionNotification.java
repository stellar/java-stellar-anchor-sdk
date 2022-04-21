package org.stellar.anchor.platform.paymentobserver;

import com.google.gson.annotations.SerializedName;
import java.time.Instant;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class CircleSubscriptionNotification {
  @SerializedName("Type")
  String type;

  @SerializedName("MessageId")
  String messageId;

  @SerializedName("Token")
  String token;

  @SerializedName("TopicArn")
  String topicArn;

  @SerializedName("Message")
  String message;

  @SerializedName("SubscribeURL")
  String subscribeUrl;

  @SerializedName("Timestamp")
  Instant Timestamp;

  @SerializedName("SignatureVersion")
  String signatureVersion;

  @SerializedName("Signature")
  String signature;

  @SerializedName("SigningCertURL")
  String signingCertURL;
}

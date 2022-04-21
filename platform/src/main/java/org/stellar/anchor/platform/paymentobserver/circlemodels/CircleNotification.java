package org.stellar.anchor.platform.paymentobserver.circlemodels;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
@Data
public class CircleNotification {
  @SerializedName("Type")
  String type;

  @SerializedName("MessageId")
  String messageId;

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
}

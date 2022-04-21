package org.stellar.anchor.platform.paymentobserver.circlemodels;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
public class CircleSubscriptionNotification extends CircleNotification {
  @SerializedName("Token")
  String token;

  @SerializedName("Message")
  String message;

  @SerializedName("SubscribeURL")
  String subscribeUrl;
}

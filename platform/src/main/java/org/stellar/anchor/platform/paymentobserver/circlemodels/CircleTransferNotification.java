package org.stellar.anchor.platform.paymentobserver.circlemodels;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
public class CircleTransferNotification extends CircleNotification {
  @SerializedName("Message")
  TransferNotificationBody message;

  @SerializedName("UnsubscribeURL")
  String unsubscribeURL;

  @SerializedName("MessageAttributes")
  Map<String, ?> messageAttributes;
}

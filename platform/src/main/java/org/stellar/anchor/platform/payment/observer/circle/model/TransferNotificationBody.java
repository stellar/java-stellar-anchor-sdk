package org.stellar.anchor.platform.payment.observer.circle.model;

import java.util.Map;
import lombok.Data;

@Data
public class TransferNotificationBody {
  String clientId;
  String notificationType;
  String version;
  Map<String, ?> customAttributes;
  CircleTransfer transfer;
}

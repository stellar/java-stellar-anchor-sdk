package org.stellar.anchor.platform.paymentobserver.circlemodels;

import lombok.Data;
import org.stellar.anchor.paymentservice.circle.model.CircleTransfer;

import java.util.Map;

@Data
public class TransferNotificationBody {
  String clientId;
  String notificationType;
  String version;
  Map<String, ?> customAttributes;
  CircleTransfer transfer;
}

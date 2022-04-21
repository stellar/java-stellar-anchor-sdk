package org.stellar.anchor.platform.paymentobserver.circlemodels;

import java.util.Map;
import lombok.Data;
import org.stellar.anchor.paymentservice.circle.model.CircleTransfer;

@Data
public class TransferNotificationBody {
  String clientId;
  String notificationType;
  String version;
  Map<String, ?> customAttributes;
  CircleTransfer transfer;
}

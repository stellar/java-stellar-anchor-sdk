package org.stellar.anchor.api.callback;

import java.time.Instant;
import lombok.Data;
import org.stellar.anchor.api.event.AnchorEvent;

@Data
public class SendEventRequest {
  // The seconds since the Unix epoch, UTC.
  Long timestamp;
  // The event payload.
  AnchorEvent payload;

  public static SendEventRequest from(AnchorEvent event) {
    SendEventRequest request = new SendEventRequest();
    request.setTimestamp(Instant.now().getEpochSecond());
    request.setPayload(event);
    return request;
  }
}

package org.stellar.anchor.api.callback;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.stellar.anchor.api.event.AnchorEvent;

/** SendEventRequest is the request body for the /event Callback API endpoint. */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SendEventRequest {
  /** The seconds since the Unix epoch, UTC. */
  Long timestamp;
  /** The event payload. */
  AnchorEvent payload;

  /**
   * Creates a SendEventRequest from an AnchorEvent.
   *
   * @param event
   * @return a SendEventRequest
   */
  public static SendEventRequest from(AnchorEvent event) {
    SendEventRequest request = new SendEventRequest();
    request.setTimestamp(Instant.now().getEpochSecond());
    request.setPayload(event);
    return request;
  }
}

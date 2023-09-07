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
  /** The ID of the event */
  String id;
  /** The seconds since the Unix epoch, UTC. */
  String timestamp;
  /** The type of event */
  String type;
  /** The event payload. */
  SendEventRequestPayload payload;

  /**
   * Creates a SendEventRequest from an AnchorEvent.
   *
   * @param event
   * @return a SendEventRequest
   */
  public static SendEventRequest from(AnchorEvent event) {
    SendEventRequest request = new SendEventRequest();
    request.setId(event.getId());
    request.setType(event.getType().toString().toLowerCase());
    request.setTimestamp(Instant.now().toString());
    request.setPayload(SendEventRequestPayload.from(event));
    return request;
  }
}

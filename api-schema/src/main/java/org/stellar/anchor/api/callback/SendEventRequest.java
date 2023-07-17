package org.stellar.anchor.api.callback;

import lombok.Data;
import org.stellar.anchor.api.event.AnchorEvent;

@Data
public class SendEventRequest {
  Long timestamp;
  AnchorEvent payload;
}

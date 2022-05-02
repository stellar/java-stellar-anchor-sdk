package org.stellar.anchor.api.event;

import lombok.Data;

@Data
public class EventRequest {
  String id;
  String type;
  Object data;
}

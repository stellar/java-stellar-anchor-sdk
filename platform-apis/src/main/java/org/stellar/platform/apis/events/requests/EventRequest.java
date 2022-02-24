package org.stellar.platform.apis.events.requests;

import lombok.Data;

@Data
public class EventRequest {
  String id;
  String type;
  Object data;
}

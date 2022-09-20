package org.stellar.anchor.platform.config;

import java.util.HashMap;
import java.util.Map;
import lombok.Data;
import org.stellar.anchor.config.EventTypeToQueueConfig;

@Data
public class PropertyEventTypeToQueueConfig implements EventTypeToQueueConfig {

  private Map<String, String> eventTypeToQueueMap;

  private String all = "ap_event_single_queue";
  private String quote_created = "ap_event_quote_created";
  private String transaction_created = "ap_event_transaction_created";
  private String transaction_status_changed = "ap_event_transaction_status_changed";
  private String transaction_error = "ap_event_transaction_error";

  public PropertyEventTypeToQueueConfig() {
    this.eventTypeToQueueMap = new HashMap<>();
    this.eventTypeToQueueMap.put("all", this.all);
    this.eventTypeToQueueMap.put("quote_created", this.quote_created);
    this.eventTypeToQueueMap.put("transaction_created", this.transaction_created);
    this.eventTypeToQueueMap.put("transaction_status_changed", this.transaction_status_changed);
    this.eventTypeToQueueMap.put("transaction_error", this.transaction_error);
  }

  @Override
  public Map<String, String> getEventTypeToQueueMap() {
    return this.eventTypeToQueueMap;
  }
}

package org.stellar.anchor.config;

import java.util.Map;

public interface EventTypeToQueueConfig {
  Map<String, String> getEventTypeToQueueMap();
}

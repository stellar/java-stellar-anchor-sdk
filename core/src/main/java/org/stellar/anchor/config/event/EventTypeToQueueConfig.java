package org.stellar.anchor.config.event;

import java.util.Map;

public interface EventTypeToQueueConfig {
  Map<String, String> getMapping();
}

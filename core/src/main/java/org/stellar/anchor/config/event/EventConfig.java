package org.stellar.anchor.config.event;

import java.util.Map;

public interface EventConfig {
  boolean isEnabled();

  /**
   * Gets the mapping from event type to the queue name.
   *
   * @return
   */
  Map<String, String> getEventTypeToQueue();

  PublisherConfig getPublisher();
}

package org.stellar.anchor.config.event;

import java.util.Map;

public interface EventConfig {
  /**
   * Indicates of the event service is enabled.
   *
   * @return
   *     <pre>true</pre>
   *     if the service is enabled;
   *     <pre>false</pre>
   *     otherwise.
   */
  boolean isEnabled();

  /**
   * Gets the mapping from event type to the queue name.
   *
   * @return the mapping from the event type to the queue name.
   */
  Map<String, String> getEventTypeToQueue();

  /**
   * Gets the publisher configuration.
   *
   * @return the publisher configuration.
   */
  PublisherConfig getPublisher();
}

package org.stellar.anchor.config.event;

public interface EventConfig {
  boolean isEnabled();

  /**
   * Gets the mapping from event type to the queue name.
   *
   * @return the EventTypeToQueueConfig object.
   */
  EventTypeToQueueConfig getEventTypeToQueue();
  PublisherConfig getPublisher();
}

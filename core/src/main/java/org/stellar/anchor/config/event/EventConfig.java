package org.stellar.anchor.config.event;

public interface EventConfig {
  /**
   * Indicates if the event service is enabled.
   *
   * @return
   *     <pre>true</pre>
   *     if the event service is enabled.
   *     <pre>false</pre>
   *     otherwise.
   */
  boolean isEnabled();

  /**
   * Gets the publisher configuration.
   *
   * @return the publisher configuration.
   */
  QueueConfig getQueue();
}

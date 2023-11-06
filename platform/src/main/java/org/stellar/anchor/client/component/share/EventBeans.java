package org.stellar.anchor.client.component.share;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.stellar.anchor.client.config.PropertyEventConfig;
import org.stellar.anchor.client.event.*;
import org.stellar.anchor.event.EventService;

@Configuration
public class EventBeans {
  /**********************************
   * Event configurations
   */
  @Bean
  @ConfigurationProperties(prefix = "events")
  PropertyEventConfig eventConfig() {
    return new PropertyEventConfig();
  }

  @Bean
  public EventService eventService(PropertyEventConfig eventConfig) {
    return new DefaultEventService(eventConfig);
  }
}

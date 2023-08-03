package org.stellar.anchor.platform.component.share;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.stellar.anchor.event.EventService;
import org.stellar.anchor.platform.config.PropertyEventConfig;
import org.stellar.anchor.platform.event.*;

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

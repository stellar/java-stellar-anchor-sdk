package org.stellar.anchor.platform.component.eventprocessor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.stellar.anchor.platform.config.EventProcessorConfig;
import org.stellar.anchor.platform.event.EventProcessor;

@Configuration
public class EventProcessorBeans {
  @Bean
  EventProcessor eventProcessor(EventProcessorConfig config) {
    return new EventProcessor();
  }
}

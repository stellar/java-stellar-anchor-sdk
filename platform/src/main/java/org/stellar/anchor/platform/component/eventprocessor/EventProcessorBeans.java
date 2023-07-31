package org.stellar.anchor.platform.component.eventprocessor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.stellar.anchor.event.EventService;
import org.stellar.anchor.platform.config.CallbackApiConfig;
import org.stellar.anchor.platform.config.ClientsConfig;
import org.stellar.anchor.platform.config.EventProcessorConfig;
import org.stellar.anchor.platform.event.EventProcessorManager;

@Configuration
public class EventProcessorBeans {
  @Bean
  EventProcessorManager eventProcessorManager(
      EventProcessorConfig eventProcessorConfig,
      CallbackApiConfig callbackApiConfig,
      ClientsConfig clientsConfig,
      EventService eventService) {
    return new EventProcessorManager(
        eventProcessorConfig, callbackApiConfig, clientsConfig, eventService);
  }
}

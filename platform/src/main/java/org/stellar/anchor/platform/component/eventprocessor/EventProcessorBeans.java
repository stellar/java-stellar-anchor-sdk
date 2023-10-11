package org.stellar.anchor.platform.component.eventprocessor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.stellar.anchor.asset.AssetService;
import org.stellar.anchor.config.SecretConfig;
import org.stellar.anchor.event.EventService;
import org.stellar.anchor.platform.config.CallbackApiConfig;
import org.stellar.anchor.platform.config.EventProcessorConfig;
import org.stellar.anchor.platform.config.PropertyClientsConfig;
import org.stellar.anchor.platform.event.EventProcessorManager;
import org.stellar.anchor.sep24.MoreInfoUrlConstructor;

@Configuration
public class EventProcessorBeans {

  @Bean
  EventProcessorManager eventProcessorManager(
      SecretConfig secretConfig,
      EventProcessorConfig eventProcessorConfig,
      CallbackApiConfig callbackApiConfig,
      PropertyClientsConfig clientsConfig,
      EventService eventService,
      AssetService assetService,
      MoreInfoUrlConstructor moreInfoUrlConstructor) {
    return new EventProcessorManager(
        secretConfig,
        eventProcessorConfig,
        callbackApiConfig,
        clientsConfig,
        eventService,
        assetService,
        moreInfoUrlConstructor);
  }
}

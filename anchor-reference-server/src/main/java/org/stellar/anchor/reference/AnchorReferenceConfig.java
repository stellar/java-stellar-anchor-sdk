package org.stellar.anchor.reference;

import com.google.gson.Gson;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.stellar.anchor.reference.config.EventSettings;
import org.stellar.anchor.reference.config.KafkaListenerSettings;
import org.stellar.anchor.reference.event.AbstractEventListener;
import org.stellar.anchor.reference.event.AnchorEventProcessor;
import org.stellar.anchor.reference.event.KafkaListener;
import org.stellar.anchor.util.GsonUtils;

@Configuration
public class AnchorReferenceConfig {
  @Bean
  public Gson gson() {
    return GsonUtils.builder().create();
  }

  @Bean
  public AbstractEventListener eventListener(
      EventSettings eventSettings,
      AnchorEventProcessor anchorEventProcessor,
      KafkaListenerSettings kafkaListenerSettings) {
    switch (eventSettings.getListenerType()) {
      case "kafka":
        return new KafkaListener(kafkaListenerSettings, anchorEventProcessor);
      case "sqs":
      case "amqp":
      default:
        throw new RuntimeException(
            String.format("Invalid event listener: %s", eventSettings.getListenerType()));
    }
  }
}

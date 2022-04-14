package org.stellar.anchor.reference.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Data
@Configuration
@ConfigurationProperties(prefix = "event")
public class EventConsumerSettings {
    String sep31Endpoint;
    String kafkaBootstrapServer;
    Map<String, String> eventTypeToQueue;
}

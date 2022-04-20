package org.stellar.anchor.server.config;


import lombok.Data;
import org.stellar.anchor.config.KafkaConfig;

import java.util.Map;

@Data
public class PropertyKafkaConfig implements KafkaConfig {
    private String kafkaBootstrapServer;
    private Map<String, String> eventTypeToQueue;
}

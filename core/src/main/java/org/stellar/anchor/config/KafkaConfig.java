package org.stellar.anchor.config;

import lombok.Data;

import java.util.Map;

public interface KafkaConfig {
    String getKafkaBootstrapServer();
    Map<String, String> getEventTypeToQueue();
}

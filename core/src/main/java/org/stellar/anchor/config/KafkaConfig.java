package org.stellar.anchor.config;

import java.util.Map;

public interface KafkaConfig {
  String getKafkaBootstrapServer();

  boolean isUseSingleQueue();

  Map<String, String> getEventTypeToQueue();
}

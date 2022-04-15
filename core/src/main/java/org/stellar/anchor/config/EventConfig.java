package org.stellar.anchor.config;

import java.util.Map;

public interface EventConfig {
  Map<String, String> getEventTypeToQueue();

  String getKafkaBootstrapServer();

  boolean isUseSingleQueue();

  boolean isEnabled();
}

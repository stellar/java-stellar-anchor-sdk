package org.stellar.anchor.config;

import java.util.Map;

public interface KafkaConfig {
  String getBootstrapServer();

  boolean isUseSingleQueue();

  Map<String, String> getEventTypeToQueue();
}

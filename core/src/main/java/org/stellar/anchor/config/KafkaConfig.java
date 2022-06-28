package org.stellar.anchor.config;

import java.util.Map;

public interface KafkaConfig {
  String getBootstrapServer();

  boolean isUseSingleQueue();

  boolean isUseIAM();

  Map<String, String> getEventTypeToQueue();
}

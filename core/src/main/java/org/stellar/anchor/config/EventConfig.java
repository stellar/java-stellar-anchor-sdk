package org.stellar.anchor.config;


import java.util.Map;

public interface EventConfig {
  Map<String, String> getQueues();

  String getKafkaBootstrapServer();

  boolean isUseSingleQueue();
}

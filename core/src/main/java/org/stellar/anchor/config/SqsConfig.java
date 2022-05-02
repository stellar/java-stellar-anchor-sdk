package org.stellar.anchor.config;

import java.util.Map;

public interface SqsConfig {
  String getRegion();

  Boolean isUseSingleQueue();

  Map<String, String> getEventTypeToQueue();

  String getAccessKey();

  String getSecretKey();
}

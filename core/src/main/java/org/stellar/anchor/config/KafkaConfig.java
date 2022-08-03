package org.stellar.anchor.config;

import java.util.Map;
import org.springframework.validation.BindException;

public interface KafkaConfig {
  String getBootstrapServer();

  boolean isUseSingleQueue();

  boolean isUseIAM();

  Map<String, String> getEventTypeToQueue();

  BindException validate();
}

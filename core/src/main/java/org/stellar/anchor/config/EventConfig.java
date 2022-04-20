package org.stellar.anchor.config;

public interface EventConfig {
  boolean isUseSingleQueue();
  boolean isEnabled();
  String getQueueType();
}


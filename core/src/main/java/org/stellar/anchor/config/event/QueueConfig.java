package org.stellar.anchor.config.event;

public interface QueueConfig {
  QueueType getType();

  enum QueueType {
    KAFKA,
    SQS,
    MSK
  }
}

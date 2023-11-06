package org.stellar.anchor.client.config;

import lombok.Data;
import org.stellar.anchor.config.event.*;

@Data
public class PropertyQueueConfig implements QueueConfig {
  QueueType type;
  KafkaConfig kafka;
  SqsConfig sqs;
  MskConfig msk;
}

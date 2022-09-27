package org.stellar.anchor.config.event;

import lombok.Data;

@Data
public class KafkaConfig implements PublisherConfigDetail {
  String bootstrapServers;
  String clientId;
  ProducerType producerType;
  int retries;
  int lingerMs;

  public enum ProducerType {
    sync,
    async
  }
}

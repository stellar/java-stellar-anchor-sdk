package org.stellar.anchor.config.event;

import lombok.Data;

@Data
public class KafkaConfig implements PublisherConfigDetail {
  /**
   * A comma-separated list of host:port pairs that are the addresses of one or more brokers in a
   * Kafka cluster, e.g. localhost:9092 or localhost:9092,another.host:9092.
   */
  String bootstrapServers;

  /** The client ID. If left empty, it is randomly generated. */
  String clientId;

  /**
   * Determines how many times the producer will attempt to send a message before marking it as
   * failed.
   */
  int retries;

  /** Determines the time to wait before sending messages out to Kafka. */
  int lingerMs;

  /** Determines the maximum amount of data to be collected before sending the batch. */
  int batchSize;
}

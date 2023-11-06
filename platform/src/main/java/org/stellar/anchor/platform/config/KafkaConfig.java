package org.stellar.anchor.platform.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class KafkaConfig {
  /**
   * A comma-separated list of host:port pairs that are the addresses of one or more brokers in a
   * Kafka cluster, e.g. localhost:9092 or localhost:9092,another.host:9092.
   */
  String bootstrapServer;

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

  /** Determines the maximum amount of time to wait for the batch to be filled. */
  int pollTimeoutSeconds;
}

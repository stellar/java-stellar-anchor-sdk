package org.stellar.anchor.platform.config;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class MskConfig extends KafkaConfig {
  boolean useIAM;

  MskConfig(
      boolean useIAM,
      String bootstrapServer,
      String clientId,
      int retires,
      int lingerMs,
      int batchSize,
      int pollTimeoutSeconds) {
    super(bootstrapServer, clientId, retires, lingerMs, batchSize, pollTimeoutSeconds);
    this.useIAM = useIAM;
  }
}

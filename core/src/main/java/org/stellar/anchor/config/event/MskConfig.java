package org.stellar.anchor.config.event;

import lombok.Data;

@Data
public class MskConfig extends KafkaConfig {
  boolean useIAM;
  String region;
}

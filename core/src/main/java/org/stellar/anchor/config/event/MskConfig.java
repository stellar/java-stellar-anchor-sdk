package org.stellar.anchor.config.event;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class MskConfig extends KafkaConfig {
  boolean useIAM;
  String awsRegion;
}

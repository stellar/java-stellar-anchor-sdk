package org.stellar.anchor.platform.config;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class MskConfig extends KafkaConfig {
  boolean useIAM;
}

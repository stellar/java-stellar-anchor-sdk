package org.stellar.anchor.platform.config;

import lombok.Data;
import org.stellar.anchor.config.event.*;

@Data
public class PropertyPublisherConfig implements PublisherConfig {
  String type;
  KafkaConfig kafka;
  SqsConfig sqs;
  MskConfig msk;
}

package org.stellar.anchor.config.event;

import lombok.Data;

@Data
public class SqsConfig implements PublisherConfigDetail {
  boolean useIAM;
  String awsRegion;
}

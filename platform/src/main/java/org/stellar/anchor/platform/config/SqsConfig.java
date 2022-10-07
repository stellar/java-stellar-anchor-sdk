package org.stellar.anchor.platform.config;

import lombok.Data;

@Data
public class SqsConfig {
  boolean useIAM;
  String awsRegion;
}

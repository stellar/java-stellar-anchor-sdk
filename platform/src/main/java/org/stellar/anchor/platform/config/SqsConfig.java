package org.stellar.anchor.platform.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SqsConfig {
  boolean useIAM;
  String awsRegion;
}

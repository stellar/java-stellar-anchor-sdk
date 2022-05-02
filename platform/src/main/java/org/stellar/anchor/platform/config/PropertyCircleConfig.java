package org.stellar.anchor.platform.config;

import lombok.Data;
import org.stellar.anchor.config.CircleConfig;

@Data
public class PropertyCircleConfig implements CircleConfig {
  String circleUrl;

  String apiKey;
}

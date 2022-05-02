package org.stellar.anchor.platform.config;

import lombok.Data;
import org.stellar.anchor.config.Sep24Config;

@Data
public class PropertySep24Config implements Sep24Config {
  boolean enabled = false;
  int interactiveJwtExpiration = 300;
  String interactiveUrl = "NA";
}

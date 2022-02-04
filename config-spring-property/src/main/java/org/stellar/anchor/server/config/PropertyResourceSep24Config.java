package org.stellar.anchor.server.config;

import lombok.Data;
import org.stellar.anchor.config.Sep24Config;

@Data
public class PropertyResourceSep24Config implements Sep24Config {
  int interactiveJwtExpiration = 300;
  String interactiveUrl = "NA";
}

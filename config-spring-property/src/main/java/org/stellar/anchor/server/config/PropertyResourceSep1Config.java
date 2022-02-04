package org.stellar.anchor.server.config;

import lombok.Data;
import org.stellar.anchor.config.Sep1Config;

@Data
public class PropertyResourceSep1Config implements Sep1Config {
  String stellarFile;
  boolean enabled = false;
}

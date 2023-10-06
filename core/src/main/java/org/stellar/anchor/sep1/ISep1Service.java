package org.stellar.anchor.sep1;

import org.stellar.anchor.config.Sep1Config;

public interface ISep1Service {
  /**
   * Read the stellar.toml content from Java resource. Override this method to read from a different
   * source.
   *
   * @param sep1Config The Sep1 configuration.
   * @return The stellar.toml content.
   * @throws Exception
   */
  String readSep1Toml(Sep1Config sep1Config) throws Exception;
}

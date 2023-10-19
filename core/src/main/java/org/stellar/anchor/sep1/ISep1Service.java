package org.stellar.anchor.sep1;

import org.stellar.anchor.config.Sep1Config;

/**
 * The interface for SEP-1 service. Use this interface to override the default implementation of the
 * Sep1Service class.
 */
public interface ISep1Service {
  /**
   * Read the stellar.toml content from Java resource. Override this method to read from a different
   * source.
   *
   * @param sep1Config The Sep1 configuration.
   * @return The stellar.toml content.
   * @throws Exception If reading the stellar.toml content fails.
   */
  String readSep1Toml(Sep1Config sep1Config) throws Exception;
}

package org.stellar.anchor.config;

public interface Sep24Config {
  boolean isEnabled();

  int getInteractiveJwtExpiration();

  String getInteractiveUrl();
}

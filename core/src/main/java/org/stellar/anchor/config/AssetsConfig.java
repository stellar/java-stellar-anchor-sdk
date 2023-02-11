package org.stellar.anchor.config;

public interface AssetsConfig {
  AssetConfigType getType();

  String getValue();

  enum AssetConfigType {
    JSON,
    YAML,
    FILE
  }
}

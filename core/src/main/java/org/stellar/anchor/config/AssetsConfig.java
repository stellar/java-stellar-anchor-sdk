package org.stellar.anchor.config;

import static org.stellar.anchor.util.StringHelper.isEmpty;

import org.stellar.anchor.api.exception.InvalidConfigException;

public interface AssetsConfig {
  AssetConfigType getType();

  String getValue();

  enum AssetConfigType {
    JSON,
    YAML,
    FILE,
    URL;

    public static AssetConfigType from(String name) throws InvalidConfigException {
      if (isEmpty(name)) throw new InvalidConfigException("Invalid asset type: " + name);
      switch (name.toLowerCase()) {
        case "json":
          return JSON;
        case "yaml":
          return YAML;
        case "file":
          return FILE;
        case "url":
          return URL;
      }
      throw new InvalidConfigException(String.format("Invalid sep1.type:[%s]", name));
    }
  }
}

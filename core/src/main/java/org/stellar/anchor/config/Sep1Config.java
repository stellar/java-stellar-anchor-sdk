package org.stellar.anchor.config;

import org.stellar.anchor.api.exception.InvalidConfigException;

@SuppressWarnings("unused")
public interface Sep1Config {
  boolean isEnabled();

  TomlType getType();

  String getValue();

  enum TomlType {
    STRING,
    FILE,
    URL;

    public static TomlType fromString(String name) throws InvalidConfigException {
      if (name == null) return null;
      switch (name.toLowerCase()) {
        case "string":
          return STRING;
        case "file":
          return FILE;
        case "url":
          return URL;
      }
      throw new InvalidConfigException(String.format("Invalid sep1.type:[%s]", name));
    }
  }
}

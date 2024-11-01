package org.stellar.anchor.config;

import static org.stellar.anchor.util.StringHelper.isEmpty;

import com.google.gson.annotations.SerializedName;
import org.stellar.anchor.api.exception.InvalidConfigException;

public interface AssetsConfig {
  AssetConfigType getType();

  String getValue();

  enum AssetConfigType {
    @SerializedName("json")
    JSON,
    @SerializedName("yaml")
    YAML,
    @SerializedName("file")
    FILE;

    public static AssetConfigType from(String name) throws InvalidConfigException {
      if (isEmpty(name)) throw new InvalidConfigException("Invalid asset type: " + name);
      switch (name.toLowerCase()) {
        case "json":
          return JSON;
        case "yaml":
          return YAML;
        case "file":
          return FILE;
      }
      throw new InvalidConfigException(String.format("Invalid sep1.type:[%s]", name));
    }
  }
}

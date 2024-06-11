package org.stellar.anchor.config;

import com.google.gson.annotations.SerializedName;

public interface ClientsConfig {
  ClientsConfigType getType();

  String getValue();

  enum ClientsConfigType {
    @SerializedName("json")
    JSON,
    @SerializedName("yaml")
    YAML,
    @SerializedName("file")
    FILE,
    @SerializedName("url")
    URL;
  }
}

package org.stellar.anchor.platform.configurator;

import static org.stellar.anchor.platform.configurator.ConfigHelper.loadConfig;
import static org.stellar.anchor.platform.configurator.ConfigMap.ConfigSource.VERSION_DEF;
import static org.stellar.anchor.util.StringHelper.isEmpty;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import org.springframework.core.io.ClassPathResource;
import org.stellar.anchor.api.exception.InvalidConfigException;
import org.stellar.anchor.platform.configurator.ConfigMap.ConfigEntry;
import org.stellar.anchor.util.Log;

public class ConfigReader {
  private final int version;
  ConfigMap configDef;

  public ConfigReader(int version) throws InvalidConfigException {
    try {
      configDef = loadConfig(new ClassPathResource(getVersionSchemaFile(version)), VERSION_DEF);
      this.version = version;
    } catch (IOException e) {
      throw new InvalidConfigException(String.format("version:%s is not a defined", version));
    }
  }

  public boolean has(String name) {
    if (configDef.getString(name) == null) {
      return false;
    }

    return isEmpty(configDef.getString(name));
  }

  public void validate(ConfigMap configMap) throws InvalidConfigException {
    List<String> errors = new LinkedList<>();
    for (String key : configMap.names()) {
      if (this.configDef.getString(key) == null) {
        errors.add(
            String.format(
                "Invalid configuration: %s=%s. (version=%d)",
                key, configMap.getString(key), version));
      }
    }

    if (errors.size() > 0) {
      throw new InvalidConfigException(errors);
    }
  }

  public ConfigMap readFrom(ConfigMap configMap) {
    ConfigMap updatedConfigMap = new ConfigMap();
    updatedConfigMap.setVersion(this.version);
    configMap
        .names()
        .forEach(
            key -> {
              ConfigEntry entry = configMap.get(key);
              String value = entry.getValue();
              String configLocation = configDef.getString(key);
              if (configLocation != null) {
                if (isEmpty(configLocation)) {
                  // Copy value
                  updatedConfigMap.put(key, value, entry.source);
                } else {
                  Log.debugF(
                      "config[{}] is moved to config[{}] in version:{}",
                      key,
                      configLocation,
                      version);
                  updatedConfigMap.put(configLocation, value, entry.source);
                }
              } else {
                Log.debugF("config[{}] is removed in version {}", key, version);
              }
            });

    return updatedConfigMap;
  }

  static String getVersionSchemaFile(Integer version) {
    return String.format("config/anchor-config-schema-v%d.yaml", version);
  }
}

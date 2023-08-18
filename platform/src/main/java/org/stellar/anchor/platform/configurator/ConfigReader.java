package org.stellar.anchor.platform.configurator;

import static org.stellar.anchor.platform.configurator.ConfigHelper.loadConfig;
import static org.stellar.anchor.platform.configurator.ConfigMap.ConfigSource.VERSION_SCHEMA;
import static org.stellar.anchor.util.StringHelper.isEmpty;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.core.io.ClassPathResource;
import org.stellar.anchor.api.exception.InvalidConfigException;
import org.stellar.anchor.platform.configurator.ConfigMap.ConfigEntry;
import org.stellar.anchor.util.Log;

public class ConfigReader {
  private final int version;
  ConfigMap configSchema;

  public ConfigReader(int version) throws InvalidConfigException {
    try {
      configSchema =
          loadConfig(new ClassPathResource(getVersionSchemaFile(version)), VERSION_SCHEMA);
      this.version = version;
    } catch (IOException e) {
      throw new InvalidConfigException(String.format("version:%s is not a defined", version));
    }
  }

  public boolean has(String name) {
    return name != null && configSchema.getString(name) != null;
  }

  public void validate(ConfigMap configMap) throws InvalidConfigException {
    List<String> errors = new LinkedList<>();
    for (String name : configMap.names()) {
      if (!isEmpty(name)) {
        // If the name is an array field, the element of the field is not validated.
        name = trimToArrayName(name);
        if (this.configSchema.getString(name) == null) {
          errors.add(
              String.format(
                  "Invalid configuration: %s=%s. (version=%d)",
                  name, configMap.getString(name), version));
        }
      }
    }
    if (errors.size() > 0) {
      throw new InvalidConfigException(errors);
    }
  }

  private final Pattern arrayPattern = Pattern.compile("\\[\\d+\\]");

  /**
   * If the name is an array field, return the name without the array index and truncate the rest of
   * the name.
   *
   * <p>Example: "assets[0].code" -> "assets"
   *
   * @param fieldName
   * @return name of the array
   */
  String trimToArrayName(String fieldName) {
    if (fieldName == null) {
      System.out.println("hello");
    }
    Matcher matcher = arrayPattern.matcher(fieldName);

    if (matcher.find()) {
      int start = matcher.start();
      return fieldName.substring(0, start);
    }
    return fieldName;
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
              String configLocation = configSchema.getString(key);
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

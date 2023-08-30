package org.stellar.anchor.platform.configurator;

import static org.stellar.anchor.platform.configurator.ConfigMap.ConfigSource.*;
import static org.stellar.anchor.util.StringHelper.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.AllArgsConstructor;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.stellar.anchor.api.exception.InvalidConfigException;
import org.stellar.anchor.platform.configurator.ConfigMap.ConfigSource;
import org.stellar.anchor.util.Log;
import org.stellar.anchor.util.StringHelper;

public class ConfigHelper {
  public static ConfigMap loadConfig(Resource resource, ConfigSource configSource)
      throws IOException {
    ConfigMap configMap = new ConfigMap();

    YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
    List<PropertySource<?>> sources = loader.load("yaml", resource);

    for (PropertySource<?> source : sources) {
      MapPropertySource mapPropertySource = (MapPropertySource) source;
      for (Map.Entry<String, Object> entry : mapPropertySource.getSource().entrySet()) {
        String key = normalize(entry.getKey());
        if (key.equals("version")) {
          configMap.setVersion(Integer.parseInt(entry.getValue().toString()));
        } else {
          configMap.put(key, entry.getValue().toString(), configSource);
        }
      }
    }
    return configMap;
  }

  public static ConfigMap loadConfigFromEnv(int latestVersion) throws InvalidConfigException {
    ConfigMap config = new ConfigMap();

    // Determine the version.
    // if system.env['VERSION'] is not defined, use the latest version.
    int version;
    String strVersion = ConfigEnvironment.getenv("VERSION");
    if (strVersion == null) {
      Log.infoF("System env['version'] is not defined. version:{} is assumed}", latestVersion);
      version = latestVersion;
    } else {
      try {
        version = Integer.parseInt(strVersion);
      } catch (NumberFormatException nfex) {
        Log.infoF(
            "System env['version']={} is invalid. All system environment variables are ignored",
            strVersion);
        return null;
      }
    }

    ConfigReader configSchema = new ConfigReader(version);
    config.setVersion(version);
    Map<String, String> posixFormToNormalizedName = new HashMap<>();

    // Maintain a map of system env variable names (POSIX standard - uppercase and underscores only)
    // to the internal config name
    for (String name : configSchema.configSchema.names()) {
      posixFormToNormalizedName.put(StringHelper.toPosixForm(name), name);
    }

    for (String envName : ConfigEnvironment.names()) {
      if (isNotEmpty(envName) && !envName.equals("VERSION")) {
        ListNameExtractResult listName = extractListNameIfAny(envName);
        if (listName == null) {
          if (configSchema.has(posixFormToNormalizedName.get(envName)))
            config.put(
                posixFormToNormalizedName.get(envName), ConfigEnvironment.getenv(envName), ENV);
        } else {
          // envName is an element of a list. Return the list name.
          config.put(
              (listName.listName + listName.index + "." + listName.elementName).toLowerCase(),
              ConfigEnvironment.getenv(envName),
              ENV);
        }
      }
    }
    return config;
  }

  @AllArgsConstructor
  static class ListNameExtractResult {
    String originalName;
    String listName;
    String index;
    String elementName;
  }

  // regex to extract the list name, index, and elementName if envName is an element of a list.
  static String regexPattern = "(.*)(\\[\\d+\\])_(.*)";
  static Pattern pattern = Pattern.compile(regexPattern);

  /**
   * Extract the list name, index, and elementName if envName is an element of a list.
   *
   * @param envName
   * @return null if envName is not an element of a list in the config schema. Otherwise, return the
   *     list name extraction result.
   */
  static ListNameExtractResult extractListNameIfAny(String envName) {
    Matcher matcher = pattern.matcher(envName);
    if (matcher.find()) {
      // envName is an element of a list. Return the list name.
      return new ListNameExtractResult(
          matcher.group(0), matcher.group(1), matcher.group(2), matcher.group(3));
    } else {
      // no match
      return null;
    }
  }

  public static String normalize(String name) {
    return camelToSnake(name);
  }

  public static ConfigMap loadDefaultConfig() throws IOException {
    return loadConfig(new ClassPathResource("config/anchor-config-default-values.yaml"), DEFAULT);
  }

  public static String suggestedSchema() throws IOException {
    ConfigMap config = loadDefaultConfig();
    for (String name : config.names()) {
      config.put(name, "", VERSION_SCHEMA);
    }

    return config.printToString();
  }

  public static void main(String[] args) throws IOException {
    System.out.println(suggestedSchema());
  }
}

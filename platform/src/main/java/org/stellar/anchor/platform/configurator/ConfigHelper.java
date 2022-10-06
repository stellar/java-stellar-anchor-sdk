package org.stellar.anchor.platform.configurator;

import static org.stellar.anchor.platform.configurator.ConfigMap.ConfigSource.*;
import static org.stellar.anchor.util.StringHelper.camelToSnake;
import static org.stellar.anchor.util.StringHelper.isEmpty;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    // if system.env['version'] is not defined, use the latest version.
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
    Map<String, String> sysEnvVarNameToNormalizedName = new HashMap<>();

<<<<<<< Updated upstream
    // Maintain a map of system env variable names (POSIX standard - uppercase and underscores only)
    // to the internal
=======
    // Maintain a map of system env variable names (POSIX standard - uppercase and underscores only) to the internal
>>>>>>> Stashed changes
    // config name
    for (String name : configSchema.configSchema.names()) {
      sysEnvVarNameToNormalizedName.put(StringHelper.toUpperSnake(name), name);
    }

    for (String name : ConfigEnvironment.names()) {
<<<<<<< Updated upstream
      if (!isEmpty(name)
          && configSchema.has(sysEnvVarNameToNormalizedName.get(name))
          && !name.equals("VERSION")) {
=======
      if (!isEmpty(name) && configSchema.has(sysEnvVarNameToNormalizedName.get(name)) && !name.equals("VERSION")) {
>>>>>>> Stashed changes
        // the envarg is defined in this version
        config.put(sysEnvVarNameToNormalizedName.get(name), ConfigEnvironment.getenv(name), ENV);
      }
    }
    return config;
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

    config.put("secret.sep10_signing_seed", "", VERSION_SCHEMA);
    config.put("secret.jwt_secret", "", VERSION_SCHEMA);
    config.put("secret.platform_api.secret", "", VERSION_SCHEMA);
    config.put("secret.callback_api.secret", "", VERSION_SCHEMA);
    config.put("secret.data.username", "", VERSION_SCHEMA);
    config.put("secret.data.password", "", VERSION_SCHEMA);

    return config.printToString();
  }

  public static void main(String[] args) throws IOException {
    System.out.println(suggestedSchema());
  }
}

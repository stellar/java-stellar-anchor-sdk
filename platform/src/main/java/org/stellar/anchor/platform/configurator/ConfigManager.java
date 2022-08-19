package org.stellar.anchor.platform.configurator;

import static org.stellar.anchor.api.platform.HealthCheckStatus.GREEN;
import static org.stellar.anchor.platform.configurator.ConfigHelper.*;
import static org.stellar.anchor.platform.configurator.ConfigMap.ConfigSource.FILE;
import static org.stellar.anchor.util.Log.*;

import com.google.gson.Gson;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
import lombok.Builder;
import lombok.Data;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.io.Resource;
import org.stellar.anchor.api.exception.InvalidConfigException;
import org.stellar.anchor.api.platform.HealthCheckResult;
import org.stellar.anchor.healthcheck.HealthCheckable;
import org.stellar.anchor.util.GsonUtils;
import org.stellar.anchor.util.Log;

public class ConfigManager
    implements ApplicationContextInitializer<ConfigurableApplicationContext>, HealthCheckable {

  static String STELLAR_ANCHOR_CONFIG = "STELLAR_ANCHOR_CONFIG";
  static ConfigManager configManager = new ConfigManager();

  ConfigMap configMap;

  private ConfigManager() {}

  public static ConfigManager getInstance() {
    return configManager;
  }

  @SneakyThrows
  @Override
  public void initialize(@NotNull ConfigurableApplicationContext applicationContext) {
    // Read configuration from system environment variables, configuration file, and default values
    info("Read and process configurations");
    configMap = processConfigurations(applicationContext);

    // Make sure no secret is leaked.
    sanitize(configMap);

    // Send values to Spring
    sendToSpring(
        applicationContext, configMap, List.of(new LogConfigAdapter(), new DataConfigAdapter()));
  }

  void sanitize(ConfigMap configMap) {
    SecretManager.getInstance()
        .secretVars
        .forEach(
            var -> {
              if (configMap.get(var) != null) {
                Log.warnF(
                    "Possible secret leak of config[{}]. Please remove the secret from configuration.",
                    var);
                configMap.remove(var);
              }
            });
  }

  void sendToSpring(
      ConfigurableApplicationContext applicationContext,
      ConfigMap config,
      List<SpringConfigAdapter> adapters)
      throws InvalidConfigException {
    Properties props = new Properties();
    props.putAll(config.map());

    // Set Platform configurations
    PropertiesPropertySource pps = new PropertiesPropertySource("config", props);
    applicationContext.getEnvironment().getPropertySources().addFirst(pps);

    for (SpringConfigAdapter adapter : adapters) {
      adapter.sendToSpring(applicationContext, config);
    }
  }

  ConfigMap processConfigurations(ConfigurableApplicationContext applicationContext)
      throws IOException, InvalidConfigException {
    info("reading default configuration values");
    // Load default values
    ConfigMap defaultConfig = loadDefaultConfig();
    ConfigMap config = defaultConfig;

    infoF("default configuration version={}", defaultConfig.getVersion());
    // Check if default config is consistent with the definition
    ConfigReader configReader = new ConfigReader(defaultConfig.getVersion());
    info("validating default configuration values");
    configReader.validate(defaultConfig);

    // Read the configuration from the YAML file specified by the STELLAR_ANCHOR_CONFIG system
    // environment variable.
    Resource configFileResource = getConfigFileAsResource(applicationContext);
    if (configFileResource != null) {
      infoF("reading configuration file from {}", configFileResource.getURL());
      ConfigMap yamlConfig = loadConfig(configFileResource, FILE);
      config.merge(applyReaders(defaultConfig, yamlConfig));
    }

    // Read and process the environment variable
    ConfigMap envConfig = loadConfigFromEnv(defaultConfig.getVersion());
    if (envConfig != null) {
      info("Processing system environment variables");
      config.merge(applyReaders(defaultConfig, envConfig));
    }

    return config;
  }

  ConfigMap applyReaders(ConfigMap defaultConfig, ConfigMap config) throws InvalidConfigException {
    int configVersion = config.getVersion();
    int latestVersion = defaultConfig.getVersion();

    ConfigReader configReader = new ConfigReader(configVersion);
    configReader.validate(config);

    for (int version = configVersion + 1; version <= latestVersion; version++) {
      infoF("Applying configuration version {}", version);
      configReader = new ConfigReader(version);
      config = configReader.readFrom(config);
    }

    return config;
  }

  Resource getConfigFileAsResource(ConfigurableApplicationContext applicationContext)
      throws IOException {
    String yamlConfigFile = ConfigEnvironment.getenv(STELLAR_ANCHOR_CONFIG);

    if (yamlConfigFile != null) {
      Resource resource = applicationContext.getResource(yamlConfigFile);
      if (!resource.exists()) {
        throw new IOException(
            String.format("Failed to read configuration from %s", yamlConfigFile));
      }
      return resource;
    }
    return null;
  }

  @Override
  public String getName() {
    return "config";
  }

  @Override
  public List<String> getTags() {
    return List.of("all", "config");
  }

  @Override
  public HealthCheckResult check() {
    Gson gson = GsonUtils.getInstance();
    return ConfigManagerHealthCheckResult.builder().name(getName()).configMap(configMap).build();
  }

  @Override
  public int compareTo(@NotNull HealthCheckable other) {
    return this.getName().compareTo(other.getName());
  }
}

@Data
@Builder
class ConfigManagerHealthCheckResult implements HealthCheckResult {
  transient String name;

  ConfigMap configMap;

  @Override
  public String name() {
    return name;
  }

  @Override
  public List<String> getStatuses() {
    return List.of();
  }

  @Override
  public String getStatus() {
    return GREEN.toString();
  }
}

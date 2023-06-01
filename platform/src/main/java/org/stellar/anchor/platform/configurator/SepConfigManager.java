package org.stellar.anchor.platform.configurator;

import static org.stellar.anchor.config.CustodyConfig.NONE_CUSTODY_TYPE;
import static org.stellar.anchor.util.Log.info;

import java.util.List;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.ConfigurableApplicationContext;
import org.stellar.anchor.api.exception.InvalidConfigException;

public class SepConfigManager extends ConfigManager {
  private static final SepConfigManager sepConfigManager = new SepConfigManager();

  private SepConfigManager() {}

  public static SepConfigManager getInstance() {
    return sepConfigManager;
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
        applicationContext,
        configMap,
        List.of(new LogConfigAdapter(), new DataConfigAdapter(), new SepServerConfigAdapter()));
  }
}

class SepServerConfigAdapter extends SpringConfigAdapter {
  @Override
  void updateSpringEnv(ConfigMap config) throws InvalidConfigException {
    copy(config, "sep_server.context_path", "server.servlet.context-path");
    copy(config, "sep_server.port", "server.port");
    set("spring.mvc.converters.preferred-json-mapper", "gson");
    if (config.getBoolean("metrics.enabled")) {
      set("management.endpoints.enabled-by-default", true);
      copy(config, "sep_server.management_server_port", "management.server.port");
      set("management.endpoints.web.exposure.include", "health,info,prometheus");
    } else {
      set("management.endpoints.enabled-by-default", false);
    }
  }

  @Override
  void validate(ConfigMap config) throws InvalidConfigException {
    String custodyType = config.getString("custody.type");
    if (!NONE_CUSTODY_TYPE.equals(custodyType)) {
      if (config.getBoolean("sep24.features.account_creation")) {
        throw new InvalidConfigException(
            "Custody service doesn't support creating accounts for users requesting deposits");
      } else if (config.getBoolean("sep24.features.claimable_balances")) {
        throw new InvalidConfigException(
            "Custody service doesn't support sending deposit funds as claimable balances");
      }
    }
  }
}

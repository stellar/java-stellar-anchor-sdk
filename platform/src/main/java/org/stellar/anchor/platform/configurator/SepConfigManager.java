package org.stellar.anchor.platform.configurator;

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
    // Enable the management server for Spring Actuator
    copy(config, "sep_server.management_server_port", "management.server.port");
    set("management.endpoints.enabled-by-default", true);
    set("management.endpoints.web.exposure.include", "health,info,prometheus");
  }

  @Override
  void validate(ConfigMap config) throws InvalidConfigException {
    // noop
  }
}

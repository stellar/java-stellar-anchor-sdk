package org.stellar.anchor.platform.configurator;

import static org.stellar.anchor.util.Log.info;

import java.util.List;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.ConfigurableApplicationContext;
import org.stellar.anchor.api.exception.InvalidConfigException;

public class ObserverConfigManager extends ConfigManager {
  private static final ObserverConfigManager observerConfigManager = new ObserverConfigManager();

  private ObserverConfigManager() {}

  public static ObserverConfigManager getInstance() {
    return observerConfigManager;
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
        List.of(new LogConfigAdapter(), new DataConfigAdapter(), new ObserverConfigAdapter()));
  }
}

class ObserverConfigAdapter extends SpringConfigAdapter {
  @Override
  void updateSpringEnv(ConfigMap config) throws InvalidConfigException {
    copy(config, "payment_observer.context_path", "server.contextPath");
    copy(config, "payment_observer.port", "server.port");
    set("spring.mvc.converters.preferred-json-mapper", "gson");
    //    set("spring.config.import", "optional:classpath:example.env[.properties]");
    set("spring.profiles.active", "stellar-observer");
  }
}

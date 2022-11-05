package org.stellar.anchor.platform.configurator;

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
    super.initialize(applicationContext);
    sendToSpring(applicationContext, configMap, List.of(new SepServerConfigAdapter()));
  }
}

class SepServerConfigAdapter extends SpringConfigAdapter {
  @Override
  void updateSpringEnv(ConfigMap config) throws InvalidConfigException {
    copy(config, "sep_server.context_path", "server.contextPath");
    copy(config, "sep_server.port", "server.port");
    set("spring.mvc.converters.preferred-json-mapper", "gson");
    if (config.getBoolean("metrics.enabled")) {
      copy(config, "sep_server.management_server_port", "server.port");
      set("management.endpoints.web.exposure.include", "health,info,prometheus");
    }
  }
}

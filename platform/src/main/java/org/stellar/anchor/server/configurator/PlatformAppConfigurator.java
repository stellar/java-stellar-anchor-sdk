package org.stellar.anchor.server.configurator;

import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.PropertiesPropertySource;

public class PlatformAppConfigurator extends AbstractConfigurator
    implements ApplicationContextInitializer<ConfigurableApplicationContext> {
  @Override
  public void initialize(@NotNull ConfigurableApplicationContext applicationContext) {
    // Find the location of the data-access settings.
    String path = getFlatProperties().getProperty("stellar.anchor.app-config.settings");

    // Load and add the data access settings to Spring `Environment`
    PropertiesPropertySource pps = createPrefixedPropertySource(path);
    applicationContext.getEnvironment().getPropertySources().addFirst(pps);
  }
}

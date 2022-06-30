package org.stellar.anchor.platform.configurator;

import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.PropertiesPropertySource;
import org.stellar.anchor.util.Log;

public class PlatformAppConfigurator extends AbstractConfigurator
    implements ApplicationContextInitializer<ConfigurableApplicationContext> {
  @Override
  public void initialize(@NotNull ConfigurableApplicationContext applicationContext) {
    // Find the location of the data-access settings.

    String path = getFlatProperties().getProperty("stellar.anchor.app-config.settings");
    Log.debug("REECEDEBUG prop path = '%s", path);
    // Load and add the data access settings to Spring `Environment`
    PropertiesPropertySource pps = createPrefixedPropertySource(path);
    Log.debug("REECEDEBUG pps = '%s", p.toString());
    applicationContext.getEnvironment().getPropertySources().addFirst(pps);
  }
}

package org.stellar.anchor.platform.configurator;

import org.jetbrains.annotations.NotNull;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.PropertiesPropertySource;

public class SpringFrameworkConfigurator extends AbstractConfigurator
    implements ApplicationContextInitializer<ConfigurableApplicationContext> {
  @Override
  public void initialize(@NotNull ConfigurableApplicationContext applicationContext) {
    // Load and add the data access settings to Spring `Environment`
    PropertiesPropertySource pps = createPrefixedPropertySource("spring");
    pps.getSource()
        .forEach(
            (String k, Object v) -> {
              if (k.startsWith("logging.level")) {
                LoggingSystem system =
                    LoggingSystem.get(SpringFrameworkConfigurator.class.getClassLoader());
                system.setLogLevel(k.replace("logging.level.", ""), LogLevel.valueOf((String) v));
              }
            });
    applicationContext.getEnvironment().getPropertySources().addFirst(pps);
  }
}

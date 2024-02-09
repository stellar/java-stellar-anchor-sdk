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
            (String cfgName, Object cfgValue) -> {
              if (cfgName.startsWith("logging.level") && cfgValue instanceof String) {
                LoggingSystem system =
                    LoggingSystem.get(SpringFrameworkConfigurator.class.getClassLoader());
                system.setLogLevel(
                    cfgName.replace("logging.level.", ""), LogLevel.valueOf((String) cfgValue));
              }
            });
    applicationContext.getEnvironment().getPropertySources().addFirst(pps);
  }
}

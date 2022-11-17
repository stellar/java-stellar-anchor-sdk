package org.stellar.anchor.platform.configurator;

import java.util.Properties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.PropertiesPropertySource;
import org.stellar.anchor.api.exception.InvalidConfigException;

/**
 * The SpringConfigAdapter is the abstract base class of the configuration adapters that reads the
 * values from the configuration and sets the Spring environment fields.
 *
 * <p>The subclass of the SpringConfigAdapter must override the sendToSpring() function to read from
 * the configMap and sets up the Spring environment properly.
 *
 * <p>The SpringConfigAdapter is NOT thread-safe.
 */
public abstract class SpringConfigAdapter {
  final Properties props = new Properties();

  protected void set(String name, boolean value) {
    props.put(name, value);
  }

  protected void set(String name, int value) {
    props.put(name, value);
  }

  protected void set(String name, String value) {
    props.setProperty(name, value);
  }

  protected void copy(ConfigMap config, String from, String to) throws InvalidConfigException {
    String value = config.getString(from);
    if (value == null) {
      throw new InvalidConfigException(String.format("config[%s] is not defined", from));
    }
    set(to, value);
  }

  protected String get(String name) {
    return props.getProperty(name);
  }

  void updateSpringEnv(ConfigurableApplicationContext applicationContext, ConfigMap config)
      throws InvalidConfigException {
    props.clear();

    updateSpringEnv(config);

    applicationContext
        .getEnvironment()
        .getPropertySources()
        .addFirst(new PropertiesPropertySource(this.getClass().getSimpleName(), props));
  }

  abstract void updateSpringEnv(ConfigMap config) throws InvalidConfigException;
}

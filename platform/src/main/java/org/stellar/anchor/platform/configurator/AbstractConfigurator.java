package org.stellar.anchor.platform.configurator;

import java.util.Properties;
import org.springframework.core.env.PropertiesPropertySource;
import org.stellar.anchor.util.PrefixedProperties;

public abstract class AbstractConfigurator {
  protected static Properties loadedProperties = new Properties();

  protected Properties getFlatProperties() {
    return loadedProperties;
  }

  protected PropertiesPropertySource createPrefixedPropertySource(String prefix) {
    PrefixedProperties prefixedProp =
        new PrefixedProperties(prefix.endsWith(".") ? prefix : prefix + ".", loadedProperties);
    return new PropertiesPropertySource(prefix, prefixedProp);
  }
}

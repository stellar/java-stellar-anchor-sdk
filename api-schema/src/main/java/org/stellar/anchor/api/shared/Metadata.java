package org.stellar.anchor.api.shared;

import java.io.IOException;
import java.util.Objects;
import java.util.Properties;

public class Metadata {
  private static Properties projectProperties;

  private static Properties getProperties() {
    if (projectProperties == null) {
      projectProperties = new Properties();
      try {
        projectProperties.load(Metadata.class.getResourceAsStream("/metadata.properties"));
      } catch (IOException ex) {
        ex.printStackTrace();
      }
    }

    return projectProperties;
  }

  private static String get(String key) {
    return get(key, null);
  }

  private static String get(String key, String defaultValue) {
    return Objects.toString(getProperties().getProperty(key), defaultValue);
  }

  public static String getVersion() {
    return get("version", "no version");
  }
}

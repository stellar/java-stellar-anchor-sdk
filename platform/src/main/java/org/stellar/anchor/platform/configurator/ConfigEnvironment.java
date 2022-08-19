package org.stellar.anchor.platform.configurator;

import static org.stellar.anchor.platform.configurator.ConfigHelper.normalize;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class ConfigEnvironment {
  static Map<String, String> env;

  static {
    reset();
  }

  public static void reset() {
    env = new HashMap<>();
    for (Map.Entry<String, String> entry : System.getenv().entrySet()) {
      env.put(normalize(entry.getKey()), entry.getValue());
    }

    Properties sysProps = System.getProperties();
    for (Map.Entry<Object, Object> entry : sysProps.entrySet()) {
      env.put(normalize(String.valueOf(entry.getKey())), String.valueOf(entry.getValue()));
    }
  }

  /**
   * This class seems redundant but it is necessary for JUnit test for creating configuration with
   * environment variables.
   *
   * @param name the name of the environment variable
   * @return the value of the environment variable.
   */
  public static String getenv(String name) {
    return env.get(ConfigHelper.normalize(name));
  }

  public static Collection<String> names() {
    return env.keySet();
  }
}

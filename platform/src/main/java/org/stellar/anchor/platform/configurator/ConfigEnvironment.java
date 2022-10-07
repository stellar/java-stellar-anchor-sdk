package org.stellar.anchor.platform.configurator;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.stellar.anchor.util.StringHelper;

public class ConfigEnvironment {
  static Map<String, String> env;

  static {
    rebuild();
  }

  public static void rebuild() {
    env = new HashMap<>();

    // Read all env variables and convert everything to POSIX form
    for (Map.Entry<String, String> entry : System.getenv().entrySet()) {
      env.put(StringHelper.toPosixForm(entry.getKey()), entry.getValue());
    }

    Properties sysProps = System.getProperties();
    for (Map.Entry<Object, Object> entry : sysProps.entrySet()) {
      env.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
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
    return env.get(name);
  }

  public static String getPosixFormEnvVal(String name) {
    return env.get(StringHelper.toPosixForm(name));
  }

  public static Collection<String> names() {
    return env.keySet();
  }
}

package org.stellar.anchor.platform.configurator;

<<<<<<< Updated upstream
=======
import org.stellar.anchor.util.StringHelper;

import static org.stellar.anchor.platform.configurator.ConfigHelper.normalize;

>>>>>>> Stashed changes
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
    env.putAll(System.getenv());

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

<<<<<<< Updated upstream
  public static String getToUpperEnv(String name) {
=======
  public static String getToUpperEnv(String name){
>>>>>>> Stashed changes
    return env.get(StringHelper.toUpperSnake(name));
  }

  public static Collection<String> names() {
    return env.keySet();
  }
}

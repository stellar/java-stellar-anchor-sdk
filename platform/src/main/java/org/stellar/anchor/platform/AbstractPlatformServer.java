package org.stellar.anchor.platform;

import java.util.Map;
import org.stellar.anchor.platform.configurator.ConfigEnvironment;

abstract class AbstractPlatformServer {
  static void buildEnvironment(Map<String, Object> environment) {
    if (environment != null) {
      for (String name : environment.keySet()) {
        System.setProperty(name, String.valueOf(environment.get(name)));
      }
      ConfigEnvironment.rebuild();
    }
  }
}

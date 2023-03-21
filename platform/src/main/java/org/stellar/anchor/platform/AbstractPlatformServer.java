package org.stellar.anchor.platform;

import java.util.Map;
import org.stellar.anchor.platform.configurator.ConfigEnvironment;

abstract class AbstractPlatformServer {
  void buildEnvironment(Map<String, String> envMap) {
    ConfigEnvironment.rebuild(envMap);
  }
}

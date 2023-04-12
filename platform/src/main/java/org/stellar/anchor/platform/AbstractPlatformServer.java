package org.stellar.anchor.platform;

import static org.stellar.anchor.util.Log.info;

import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.stellar.anchor.platform.configurator.ConfigEnvironment;

abstract class AbstractPlatformServer {
  ConfigurableApplicationContext ctx;

  void buildEnvironment(Map<String, String> envMap) {
    info("Building Anchor Platform environment...");
    ConfigEnvironment.rebuild(envMap);
  }

  public void stop() {
    if (ctx != null) {
      SpringApplication.exit(ctx);
      ctx = null;
    }
  }
}

package org.stellar.anchor.sep1;

import static org.stellar.anchor.util.Log.debug;
import static org.stellar.anchor.util.Log.debugF;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.stellar.anchor.config.Sep1Config;
import org.stellar.anchor.util.Log;
import org.stellar.anchor.util.NetUtil;

public class Sep1Service {
  private String tomlValue;

  /**
   * Construct the Sep1Service that reads the stellar.toml content from Java resource.
   *
   * @param sep1Config The Sep1 configuration.
   */
  public Sep1Service(Sep1Config sep1Config) throws IOException {
    if (sep1Config.isEnabled()) {
      debug("sep1Config:", sep1Config);
      switch (sep1Config.getType().toLowerCase()) {
        case "string":
          debug("reading stellar.toml from config[sep1.toml.value]");
          tomlValue = sep1Config.getValue();
          break;
        case "file":
          debugF("reading stellar.toml from {}", sep1Config.getValue());
          tomlValue = Files.readString(Path.of(sep1Config.getValue()));
          break;
        case "url":
          debugF("reading stellar.toml from {}", sep1Config.getValue());
          tomlValue = NetUtil.fetch(sep1Config.getValue());
          break;
      }

      Log.info("Sep1Service initialized.");
    }
  }

  public String getStellarToml() {
    return tomlValue;
  }
}

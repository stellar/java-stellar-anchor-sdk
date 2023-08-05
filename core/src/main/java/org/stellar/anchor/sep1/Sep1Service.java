package org.stellar.anchor.sep1;

import static org.stellar.anchor.util.Log.debugF;
import static org.stellar.anchor.util.Log.infoF;

import java.io.IOException;
import java.nio.file.Path;
import org.stellar.anchor.api.exception.InvalidConfigException;
import org.stellar.anchor.api.exception.SepNotFoundException;
import org.stellar.anchor.config.Sep1Config;
import org.stellar.anchor.util.FileUtil;
import org.stellar.anchor.util.NetUtil;

public class Sep1Service {

  private String tomlValue;

  public Sep1Service(Sep1Config sep1Config) throws SepNotFoundException, InvalidConfigException {
    if (sep1Config.isEnabled()) {
      debugF("sep1Config: {}", sep1Config);
      this.tomlValue = handleConfigType(sep1Config);
      infoF("Sep1Service initialized.");
    }
  }

  private String handleConfigType(Sep1Config sep1Config)
      throws SepNotFoundException, InvalidConfigException {
    switch (sep1Config.getType()) {
      case STRING:
        debugF("reading stellar.toml from config[sep1.toml.value]");
        return sep1Config.getValue();
      case FILE:
        debugF("reading stellar.toml from {}", sep1Config.getValue());
        return readFile(sep1Config);
      case URL:
        debugF("reading stellar.toml from {}", sep1Config.getValue());
        return fetchUrl(sep1Config);
      default:
        throw new InvalidConfigException(
            String.format("invalid sep1.type: %s", sep1Config.getType()));
    }
  }

  private String readFile(Sep1Config sep1Config) throws SepNotFoundException {
    try {
      return FileUtil.read(Path.of(sep1Config.getValue()));
    } catch (IOException e) {
      throw new SepNotFoundException(
          String.format("stellar.toml not found at %s", sep1Config.getValue()));
    }
  }

  private String fetchUrl(Sep1Config sep1Config) throws SepNotFoundException {
    try {
      return NetUtil.fetch(sep1Config.getValue());
    } catch (IOException e) {
      throw new SepNotFoundException(
          String.format("stellar.toml not found at %s", sep1Config.getValue()));
    }
  }

  public String getStellarToml() {
    return tomlValue;
  }
}

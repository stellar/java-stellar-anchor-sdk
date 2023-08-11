package org.stellar.anchor.util;

import static org.stellar.anchor.util.Log.*;

import com.moandjiezana.toml.Toml;
import java.io.IOException;
import org.stellar.anchor.api.exception.InvalidConfigException;

public class Sep1Helper {
  public static TomlContent readToml(String url) throws IOException {
    try {
      String tomlValue = NetUtil.fetch(url);
      return new TomlContent(tomlValue);
    } catch (IOException e) {
      String obfuscatedMessage =
          String.format("An error occurred while fetching the TOML from %s", url);
      Log.error(e.toString());
      throw new IOException(obfuscatedMessage); // Preserve the original exception as the cause
    }
  }

  public static TomlContent parse(String tomlString) throws InvalidConfigException {
    try {
      return new TomlContent(tomlString);
    } catch (Exception e) {
      // Obfuscate the message and rethrow
      String obfuscatedMessage = "Failed to parse TOML content. Invalid Config.";
      Log.error(e.toString()); // Log the parsing exception
      throw new InvalidConfigException(
          obfuscatedMessage); // Preserve the original exception as the cause

    }
  }

  public static class TomlContent {
    private final Toml toml;

    TomlContent(String tomlString) {
      toml = new Toml().read(tomlString);
    }

    public String getString(String key) {
      return toml.getString(key);
    }

    public String getString(String key, String defaultValue) {
      return toml.getString(key, defaultValue);
    }
  }
}

package org.stellar.anchor.util;

import com.moandjiezana.toml.Toml;
import java.io.IOException;
import java.net.URL;

public class Sep1Helper {
  public static TomlContent readToml(String url) throws IOException {
    return new TomlContent(new URL(url));
  }

  public static TomlContent parse(String tomlString) {
    return new TomlContent(tomlString);
  }

  public static class TomlContent {
    private final Toml toml;

    TomlContent(URL url) throws IOException {
      String tomlValue = NetUtil.fetch(url.toString());
      toml = new Toml().read(tomlValue);
    }

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

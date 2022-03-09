package org.stellar.anchor.util;

import com.moandjiezana.toml.Toml;
import java.io.IOException;

public class Sep1Helper {
  public static TomlContent readToml(String url) throws IOException {
    return new TomlContent(url);
  }

  public static class TomlContent {
    private Toml toml;

    TomlContent(String url) throws IOException {
      String tomlValue = NetUtil.fetch(url);
      toml = new Toml().read(tomlValue);
    }

    public String getString(String key) {
      return toml.getString(key);
    }

    public String getString(String key, String defaultValue) {
      return toml.getString(key, defaultValue);
    }
  }
}

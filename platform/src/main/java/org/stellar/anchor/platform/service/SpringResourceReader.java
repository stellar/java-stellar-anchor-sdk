package org.stellar.anchor.platform.service;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import org.apache.commons.io.IOUtils;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.stellar.anchor.util.ResourceReader;

public class SpringResourceReader implements ResourceReader {
  final ResourceLoader resourceLoader = new DefaultResourceLoader();

  @Override
  public String readResourceAsString(String path) {
    return asString(resourceLoader.getResource(path));
  }

  @Override
  public boolean checkResourceExists(String path) {
    return resourceLoader.getResource(path).exists();
  }

  public String asString(Resource resource) {
    try (Reader reader = new InputStreamReader(resource.getInputStream(), UTF_8)) {
      return IOUtils.toString(reader);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
